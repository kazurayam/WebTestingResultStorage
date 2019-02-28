package com.kazurayam.materials.stats

import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import javax.imageio.ImageIO

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.FileType
import com.kazurayam.materials.Helpers
import com.kazurayam.materials.ImageDeltaStats
import com.kazurayam.materials.Material
import com.kazurayam.materials.MaterialStorage
import com.kazurayam.materials.TCaseResult
import com.kazurayam.materials.TSuiteName
import com.kazurayam.materials.TSuiteResult
import com.kazurayam.materials.TSuiteResultId
import com.kazurayam.materials.TSuiteTimestamp
import com.kazurayam.materials.imagedifference.ImageDifference

import groovy.json.JsonOutput

/**
 * 
 * @author kazurayam
 */
class StorageScanner {
    
    static Logger logger_ = LoggerFactory.getLogger(StorageScanner.class)
    
    private MaterialStorage materialStorage_
    private Options options_
    private BufferedImageBuffer biBuffer_
    
    private ImageDeltaStats imageDeltaStats_
    
    public StorageScanner(MaterialStorage materialStorage) {
        this(materialStorage, new Options.Builder().build())
    }
    
    public StorageScanner(MaterialStorage materialStorage, Options options) {
        this.materialStorage_ = materialStorage
        this.options_ = options
        this.biBuffer_ = new BufferedImageBuffer()
        // speed up ImageIO!
        ImageIO.setUseCache(false)
        //
        if ( ! options_.getPreviousImageDeltaStats().equals("") ) {
            /*
             * We will try to open the previos image-delta-stats.json file.
             * Even if failed to open, we will just ignore it and continue.
             */
            try {
                Path path = Paths.get(options_.getPreviousImageDeltaStats())
                imageDeltaStats_ = ImageDeltaStatsImpl.fromJsonFile(path)
            } catch (Exception ex) {
                logger_.warn("As previousImageDeltaStats, failed to read ${options_.getPreviousImageDeltaStats()}, will be ignored to continue")
                imageDeltaStats_ = null
            }
        } else {
            imageDeltaStats_ = null
        }
    }
    
    /**
     * 
     * @return Options object which is in use
     */
    Options getOptions() {
        return this.options_
    }
    
    /**
     * This will return
     * <PRE>
     * {
     *  "defaultCriteriaPercentage":5.0,
     *  "statsEntryList":[
     *      // a StatsEntry object of the TSuiteName specified
     *  ]
     * }
     * </PRE>
     *
     * @param materialStorage
     * @return a ImageDeltaStats object
     */
    ImageDeltaStats scan(TSuiteName tSuiteName) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        
        ImageDeltaStatsImpl.Builder builder = 
            new ImageDeltaStatsImpl.Builder().storageScannerOptions(options_)
        //
        if (materialStorage_.getTSuiteNameList().contains(tSuiteName)) {
            StatsEntry se = this.makeStatsEntry(tSuiteName)
            builder.addImageDeltaStatsEntry(se)
        } else {
            logger_.warn("No ${tSuiteName} is found in ${materialStorage_}")
        }
        ImageDeltaStats ids = builder.build()
        
        stopWatch.stop()
        logger_.debug("#scan(${tSuiteName}) took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return ids
    }
        
    /**
     * This will return
     * <PRE>
     *     {
     *          "TSuiteName": "47News_chronos_capture",
     *          "materialStatsList": [
     *              // list of MaterialStats objects
     *          ]
     *     }
     * </PRE>
     * 
     * @param ms
     * @param tSuiteName
     * @return a StatsEntry object
     */
    StatsEntry makeStatsEntry(TSuiteName tSuiteName) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        StatsEntry statsEntry = new StatsEntry(tSuiteName)
        Set<Path> set = 
            materialStorage_.getSetOfMaterialPathRelativeToTSuiteTimestamp(tSuiteName)
        for (Path path : set) {
            MaterialStats materialStats = this.makeMaterialStats(tSuiteName, path)
            statsEntry.addMaterialStats(materialStats)
        }
        stopWatch.stop()
        logger_.debug("#makeStatsEntry(${tSuiteName}) took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return statsEntry
    }

    
    /**
     * This will return
     * <PRE>
     *              {
     *                  "path: "main.TC_47News.visitSite/47NEWS_TOP.png",
     *                  "imageDeltaList": [
     *                      // list of ImageDelta objects
     *                  ],
     *                  
     *              }
     * </PRE>
     * 
     * @param ms
     * @param pathRelativeToTSuiteTimestamp
     * @return
     */
    MaterialStats makeMaterialStats(TSuiteName tSuiteName,
                                Path pathRelativeToTSuiteTimestamp) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        
        // At first, look up materials of FileType.PNG 
        //     within the TSuiteName across multiple TSuiteTimestamps
        // This list is sorted by descending order of TSuiteTimestamp
        List<Material> materials = this.getMaterialsOfARelativePathInATSuiteName(
                                        tSuiteName,
                                        pathRelativeToTSuiteTimestamp)
        
        // build the MaterialStats object while calculating the diff ratio 
        // of two PNG files
        List<ImageDelta> imageDeltaList = new ArrayList<ImageDelta>()
        if (materials.size() > 1) {
            for (int i = 0;
                    i < materials.size() - 1 &&
                    i < options_.getMaximumNumberOfImageDeltas();
                    i++) {
                // the following 1 line requires much calcuration resources
                ImageDelta imageDelta = this.makeImageDelta(materials.get(i), materials.get(i + 1))
                //
                imageDeltaList.add(imageDelta)
            }
        }
        MaterialStats materialStats  = new MaterialStats(
                    pathRelativeToTSuiteTimestamp, imageDeltaList)
        
        // configure parameters
        materialStats.setFilterDataLessThan(options_.getFilterDataLessThan())
        materialStats.setShiftCriteriaPercentageBy(options_.getShiftCriteriaPercentageBy())
        
        //
        stopWatch.stop()
        logger_.debug("#makeMaterialStats(${tSuiteName},${pathRelativeToTSuiteTimestamp} " + 
            "took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return materialStats
    }

    /**
     *
     * @param tSuiteName
     * @param pathRelativeToTSuiteTimestamp
     * @return
     */
    List<Material> getMaterialsOfARelativePathInATSuiteName(
                                TSuiteName tSuiteName,
                                Path pathRelativeToTSuiteTimestamp) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        List<Material> materialList = new ArrayList<Material>()
        // construct a list of Materials
        List<TSuiteResultId> idsOfTSuiteName = materialStorage_.getTSuiteResultIdList(tSuiteName)
        for (TSuiteResultId tSuiteResultId : idsOfTSuiteName) {
            TSuiteResult tSuiteResult = materialStorage_.getTSuiteResult(tSuiteResultId)
            for (Material mate: tSuiteResult.getMaterialList()) {
                if (mate.fileType.equals(FileType.PNG) &&
                    mate.getPathRelativeToTSuiteTimestamp() == pathRelativeToTSuiteTimestamp) {
                    if (this.isInRangeOfTSuiteTimestamp(mate)) {
                        materialList.add(mate)
                    }
                }
            }
        }
        // sort the Material list by the descending order of TSuiteTimestamp
        Collections.sort(materialList, new Comparator<Material>() {
            public int compare(Material materialA, Material materialB) {
                TSuiteResult tsrA = materialA.getTCaseResult().getParent()
                TSuiteResult tsrB = materialB.getTCaseResult().getParent()
                if (tsrA > tsrB) {
                    return -1
                } else if (tsrA == tsrB) {
                    Path pathA = materialA.getPath()
                    Path pathB = materialB.getPath()
                    return pathA.compareTo(pathB)
                } else {
                    return 1
                }
            }
        })
        stopWatch.stop()
        logger_.debug("#getMaterialsOfARelativePathInATSuiteName(${tSuiteName},${pathRelativeToTSuiteTimestamp} " +
            "took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return materialList
    }

    /**
     * Read 2 PNG files to get image difference, calculate the diff ratio,
     * and will return a ImageDelta object.
     * 
     * Please note that this method call takes fairly long processing time (2 to 4 seconds).
     * 
     * <PRE>
     *      { "a": "20190216_064354", "b": "20190216_064149", "delta": 0.10 }
     * </PRE>
     * 
     * @param a
     * @param b
     * @return a ImageDelta object
     */
    ImageDelta makeImageDelta(Material a, Material b) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        Objects.requireNonNull(a, "Material a must not be null")
        Objects.requireNonNull(b, "Material b must not be null")
        if (a.getFileType() != FileType.PNG) {
            throw new IllegalArgumentException("${a.path()} is not a PNG file")
        }
        if (b.getFileType() != FileType.PNG) {
            throw new IllegalArgumentException("${b.path()} is not a PNG file")
        }
        //
        TSuiteTimestamp tSuiteTimestampA = a.getParent().getParent().getTSuiteTimestamp()
        TSuiteTimestamp tSuiteTimestampB = b.getParent().getParent().getTSuiteTimestamp()
        // read PNG files and
        // create ImageDifference of the 2 given images to calculate the diff ratio
        BufferedImage biA = biBuffer_.read(a)
        BufferedImage biB = biBuffer_.read(b)
        // Here we use our greatest magic!
        ImageDifference diff = new ImageDifference(biA, biB)
        // make the delta
        ImageDelta imageDelta = new ImageDelta(tSuiteTimestampA, tSuiteTimestampB, diff.getRatio())
        biBuffer_.remove(a)    // a will be no longer used, b will be reused once again
        
        stopWatch.stop()
        logger_.debug("#makeImageDelta(${a}, ${b}) " +
            "took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return imageDelta
    }
    
    /**
     * Check if the Material is newer than the TSuiteTimestamp specified by the onlySince property 
     * in the Options object passed to the StorageScanner constructor.
     * The boolean property onlySinceInclusive is respected.
     *  
     * @param material
     * @return
     */
    boolean isInRangeOfTSuiteTimestamp(Material material) {
        TSuiteTimestamp onlySince = options_.getOnlySince()
        boolean onlySinceInclusive = options_.getOnlySinceInclusive()
        if (onlySince != TSuiteTimestamp.NULL) {
            TCaseResult tcr = material.getParent()
            TSuiteResult tsr = tcr.getParent()
            TSuiteTimestamp tst = tsr.getTSuiteTimestamp()
            logger_.debug("#isInRangeOfTSuiteTimestamp onlySinceInclusive=${onlySinceInclusive}, tst=${tst.toString()}, onlySince=${onlySince.toString()}")
            if (onlySinceInclusive) {
                return tst >= onlySince    
            } else {
                return tst > onlySince
            }
        } else {
            // if onlySince is not specified, return true
            return true
        }
    }
    
    /**
     * This class mainteins a buffer of BufferedImage to make I/O to PNG files efficient.
     */
    static class BufferedImageBuffer {
        private Map<Material, BufferedImage> buffer
        BufferedImageBuffer() {
            buffer = new HashMap<Material, BufferedImage>()
        }
        BufferedImage read(Material material) {
            if (!buffer.containsKey(material)) {
                BufferedImage bi = ImageIO.read(material.getPath().toFile())
                buffer.put(material, bi)
            }
            return buffer.get(material)
        }
        BufferedImage remove(Material material) {
            return buffer.remove(material)
        }
        int size() {
            return buffer.size()
        }
    }
    
    /**
     * Options to control the behavior of StroageScanner
     */
    static class Options {
        
        private double shiftCriteriaPercentageBy
        private double filterDataLessThan
        private double probability
        private int maximumNumberOfImageDeltas
        private TSuiteTimestamp onlySince
        private boolean onlySinceInclusive
        private String previousImageDeltaStats
        
        static class Builder {
            private double shiftCriteriaPercentageBy
            private double filterDataLessThan
            private double probability
            private int maximumNumberOfImageDeltas
            private TSuiteTimestamp onlySince
            private boolean onlySinceInclusive
            private String previousImageDeltaStats
            
            /*
             * constructor, where we set the default values
             */
            Builder() {
                this.shiftCriteriaPercentageBy = MaterialStats.SUGGESTED_SHIFT_CRITERIA_PERCENTAGE_BY
                this.filterDataLessThan = MaterialStats.DEFAULT_FILTER_DATA_LESS_THAN
                this.probability = MaterialStats.DEFAULT_PROBABILITY
                this.maximumNumberOfImageDeltas = MaterialStats.DEFAULT_MAXIMUM_NUMBER_OF_IMAGEDELTAS
                this.onlySince = new TSuiteTimestamp('19990101_000000')
                this.onlySinceInclusive = true
                this.previousImageDeltaStats = ""
            }
            Builder shiftCriteriaPercentageBy(double value) {
                if (value < 0.0) {
                    throw new IllegalArgumentException("shiftCriteriaPercentageBy must not be negative")
                }
                if (value > 100.0) {
                    throw new IllegalArgumentException("shiftCriteriaPercentageBy must not be > 100.0")
                }
                this.shiftCriteriaPercentageBy = value
                return this
            }
            Builder filterDataLessThan(double value) {
                if (value < 0.0) {
                    throw new IllegalArgumentException("filterDataLessThan must not be negative")
                }
                if (value > 100.0) {
                    throw new IllegalArgumentException("filterDataLessThan must not be  > 100.0")
                }
                this.filterDataLessThan = value
                return this
            }
            Builder probability(double value) {
                if (value < 0.0) {
                    throw new IllegalArgumentException("probability must not be negative")
                }
                if (value > 1.0) {
                    throw new IllegalArgumentException("probability must not be > 1.0")
                }
                this.probability = value
                return this
            }
            Builder maximumNumberOfImageDeltas(int value) {
                if (value < 1) {
                    throw new IllegalArgumentException("maximumNumberOfImageDeltas must not be less than 1")
                }
                this.maximumNumberOfImageDeltas = value
                return this
            }
            Builder onlySince(TSuiteTimestamp tSuiteTimestamp, boolean inclusive = true) {
                this.onlySince = tSuiteTimestamp
                this.onlySinceInclusive = inclusive
                return this
            }
            Builder previousImageDeltaStats(String path) {
                this.previousImageDeltaStats = path
                return this
            }
            Options build() {
                return new Options(this)
            }
        }
        
        private Options(Builder builder) {
            this.shiftCriteriaPercentageBy = builder.shiftCriteriaPercentageBy
            this.filterDataLessThan = builder.filterDataLessThan
            this.probability = builder.probability
            this.maximumNumberOfImageDeltas = builder.maximumNumberOfImageDeltas
            this.onlySince = builder.onlySince
            this.onlySinceInclusive = builder.onlySinceInclusive
            this.previousImageDeltaStats = builder.previousImageDeltaStats
        }
        
        double getShiftCriteriaPercentageBy() {
            return this.shiftCriteriaPercentageBy
        }
        
        double getFilterDataLessThan() {
            return this.filterDataLessThan
        }
        
        double getProbability() {
            return this.probability
        }
        
        int getMaximumNumberOfImageDeltas() {
            return this.maximumNumberOfImageDeltas
        }
        
        TSuiteTimestamp getOnlySince() {
            return this.onlySince
        }
        
        boolean getOnlySinceInclusive() {
            return this.onlySinceInclusive    
        }
        
        String getPreviousImageDeltaStats() {
            return this.previousImageDeltaStats
        }
        
        @Override
        String toString() {
            return this.toJsonText()
        }
        
        String toJsonText() {
            StringBuilder sb = new StringBuilder()
            sb.append("{")
            //
            sb.append("\"shiftCriteriaPercentageBy\":")
            sb.append(this.getShiftCriteriaPercentageBy())
            sb.append(",")
            //
            sb.append("\"filterDataLessThan\":")
            sb.append(this.getFilterDataLessThan())
            sb.append(",")
            //
            sb.append("\"maximumNumberOfImageDeltas\":")
            sb.append(this.getMaximumNumberOfImageDeltas())
            sb.append(",")
            //
            sb.append("\"onlySince\":")
            sb.append("\"" + this.getOnlySince().format() + "\"")
            sb.append(",")
            //
            sb.append("\"onlySinceInclusive\":")
            sb.append(this.getOnlySinceInclusive())
            sb.append(",")
            //
            sb.append("\"probability\":")
            sb.append(this.getProbability())
            sb.append(",")
            //
            sb.append("\"previousImageDeltaStats\":")
            if (this.getPreviousImageDeltaStats() != null) {
                sb.append("\"")
                sb.append(Helpers.escapeAsJsonText(this.getPreviousImageDeltaStats().toString()))
                sb.append("\"")
            } else {
                sb.append("\"\"")
            }
            //
            sb.append("}")
            sb.toString()
        }
    }
}
