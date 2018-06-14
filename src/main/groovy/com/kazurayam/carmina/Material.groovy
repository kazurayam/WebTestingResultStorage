package com.kazurayam.carmina

import java.nio.file.Path

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Material {

    static Logger logger = LoggerFactory.getLogger(Material.class)

    protected static final String MAGIC_DELIMITER = '§'

    private TargetURL parent
    private Path materialFilePath
    private FileType fileType

    Material(Path materialFilePath, FileType fileType) {
        this.materialFilePath = materialFilePath
        this.fileType = fileType
    }

    Material setParent(TargetURL parent) {
        this.parent = parent
        return this
    }

    TargetURL getParent() {
        return this.getTargetURL()
    }

    TargetURL getTargetURL() {
        return this.parent
    }

    Path getMaterialFilePath() {
        return materialFilePath
    }

    FileType getFileType() {
        return fileType
    }

    Path getRelativePathToTsTimestampDir() {
        if (parent != null) {
            Path tsTimestampDir =
                this.getTargetURL().getTCaseResult().getTSuiteResult().getTSuiteTimestampDir()
            Path path = tsTimestampDir.relativize(this.materialFilePath).normalize()
            return path
        } else {
            def msg = "parent TargetURL is null"
            logger.error(msg)
            throw new IllegalStateException(msg)
        }
    }

    /**
     * relative path to the TestSuiteName/Timestamp directory
     */
    String getRelativePathAsString() {
        return this.getRelativePathToTsTimestampDir().toString()
    }

    /**
     *
     * @return
     */
    String getRelativeUrlAsString() {
        return this.getRelativePathAsString().replace('\\','/').replace('%','%25')
    }

    // ---------------- helpers -----------------------------------------------
    static FileType parseFileNameForFileType(String fileName) {
        String[] arr = fileName.split('\\.')
        if (arr.length < 2) {
            return FileType.NULL
        } else {
            Arrays.sort(arr, Collections.reverseOrder())
            String candidate = arr[0]
            try {
                FileType ft = FileType.getByExtension(candidate)
                return ft
            } catch (IllegalArgumentException e) {
                logger.info("unknown file extension '${candidate}' in the file name '${fileName}'")
                return FileType.NULL
            }
        }
    }

    static Suffix parseFileNameForSuffix(String fileName) {
        FileType ft = parseFileNameForFileType(fileName)
        if (ft != FileType.NULL) {
            String str = fileName.substring(0, fileName.lastIndexOf('.'))
            String[] arr = str.split(Material.MAGIC_DELIMITER)
            if (arr.length < 2) {
                return null
            }
            if (arr.length > 3) {
                logger.warn("${fileName} contains 2 or more ${Material.MAGIC_DELIMITER} character. " +
                        "Valid but unexpected.")
            }
            Arrays.sort(arr, Collections.reverseOrder())
            return new Suffix(arr[0])
        } else {
            return null
        }
    }

    static URL parseFileNameForURL(String fileName) {
        FileType ft = parseFileNameForFileType(fileName)
        if (ft != FileType.NULL) {
            Suffix suffix = parseFileNameForSuffix(fileName)
            String urlstr
            if (suffix != null) {
                urlstr = fileName.substring(0, fileName.lastIndexOf(Material.MAGIC_DELIMITER))
            } else {
                urlstr = fileName.substring(0, fileName.lastIndexOf('.'))
            }
            String decoded = URLDecoder.decode(urlstr, 'UTF-8')
            try {
                URL url = new URL(decoded)
                return url
            } catch (MalformedURLException e) {
                logger.warn("unknown protocol in '${decoded}'")
                return null
            }
        } else {
            return null
        }
    }

    static String resolveMaterialFileName(URL url, Suffix suffix, FileType fileType) {
        String encodedUrl = URLEncoder.encode(url.toExternalForm(), 'UTF-8')
        if (suffix != Suffix.NULL) {
            return "${encodedUrl}${Material.MAGIC_DELIMITER}${suffix.toString()}.${fileType.getExtension()}"
        } else {
            return "${encodedUrl}.${fileType.getExtension()}"
        }
    }

    // ---------------- overriding Object properties --------------------------
    @Override
    boolean equals(Object obj) {
        //if (this == obj) { return true }
        if (!(obj instanceof Material)) { return false }
        Material other = (Material)obj
        return this.materialFilePath == other.getMaterialFilePath()
    }

    @Override
    int hashCode() {
        final int prime = 31
        int result = 1
        result = prime * result + this.getTargetURL().hashCode()
        result = prime * result + this.getMaterialFilePath().hashCode()
        return result
    }

    @Override
    String toString() {
        return this.toJson()
    }

    String toJson() {
        StringBuilder sb = new StringBuilder()
        sb.append('{')
        sb.append('"Material":{')
        sb.append('"materialFilePath":"' + Helpers.escapeAsJsonText(this.materialFilePath.toString()) + '",')
        sb.append('"fileType":"' + Helpers.escapeAsJsonText(this.fileType.toString()) + '"')
        sb.append('}}')
        return sb.toString()
    }
}