package com.kazurayam.materials.imagedifference


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.TExecutionProfile
import com.kazurayam.materials.ReportsAccessor

import com.kazurayam.materials.view.ExecutionPropertiesWrapper
import com.kazurayam.materials.Material
import com.kazurayam.materials.TSuiteResult

/**
 *
 * @author kazurayam
 *
 */
class ImageDifferenceFilenameResolverDefaultImpl implements ImageDifferenceFilenameResolver {
    
    static Logger logger_ = LoggerFactory.getLogger(ImageDifferenceFilenameResolverDefaultImpl.class)
    
    private ReportsAccessor reportsAccessor_
    
    ImageDifferenceFilenameResolverDefaultImpl(ReportsAccessor reportsAccessor) {
        Objects.requireNonNull(reportsAccessor, "reportsAccessor must not be null")
        this.reportsAccessor_ = reportsAccessor
    }
    
    /**
     * Given with the following arguments:
     *     Material expMate:                 'Materials/Main/TS1/20181014_131314/CURA_Homepage' created by TSuiteResult with 'product' profile
     *     Material actMate:                 'Materials/Main/TS1/20181014_131315/CURA_Homepage' created by TSuiteResult with 'develop' profile
     *     ImageDifference:                  6.71
     *     Double criteriaPercent:           3.0
     *
     * @return 'CURA_Homepage.20181014_131314_product-20181014_131315_develop.(6.71)FAILED.png'
     */
    String resolveImageDifferenceFilename(
            Material expMate,
            Material actMate,
            ImageDifference diff,
            double criteriaPercent) {
        
        TSuiteResult tSuiteResultExpected = expMate.getParent().getParent()
        TSuiteResult tSuiteResultActual   = actMate.getParent().getParent()
        logger_.debug("#resolveImageDifferenceFilename tSuiteResultExpected=${tSuiteResultExpected}")
        logger_.debug("#resolveImageDifferenceFilename tSuiteResultActual  =${tSuiteResultActual}")
        
        ExecutionPropertiesWrapper epwExpected = reportsAccessor_.getExecutionPropertiesWrapper(tSuiteResultExpected.getId())
        ExecutionPropertiesWrapper epwActual   = reportsAccessor_.getExecutionPropertiesWrapper(tSuiteResultActual.getId())
        logger_.debug("#resolveImageDifferenceFilename epwExpected=${epwExpected}")
        logger_.debug("#resolveImageDifferenceFilename epwActual  =${epwActual}")
        
        TExecutionProfile profileExpected = (epwExpected != null) ? epwExpected.getTExecutionProfile() : TExecutionProfile.BLANK
        TExecutionProfile profileActual   = (epwActual   != null) ? epwActual.getTExecutionProfile()   : TExecutionProfile.BLANK
        logger_.debug("#resolveImageDifferenceFilename profileExpected=${profileExpected}")
        logger_.debug("#resolveImageDifferenceFilename profileActual  =${profileActual}")
        
        //
        String fileName = expMate.getPath().getFileName().toString()
        String fileId = fileName.substring(0, fileName.lastIndexOf('.'))
        String expTimestamp = expMate.getParent().getParent().getTSuiteTimestamp().format()
        String actTimestamp = actMate.getParent().getParent().getTSuiteTimestamp().format()
        //
        StringBuilder sb = new StringBuilder()
        sb.append("${fileId}.")
        sb.append("${expTimestamp}_${profileExpected}")
        sb.append("-")
        sb.append("${actTimestamp}_${profileActual}")
        sb.append(".")
        sb.append("(${diff.getRatioAsString()})")
        sb.append("${(diff.imagesAreSimilar(criteriaPercent)) ? '.png' : 'FAILED.png'}")
        return sb.toString()
    }
}
