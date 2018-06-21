package com.kazurayam.carmina

import java.nio.file.Path

interface TestMaterialsRepository {

    void putCurrentTestSuite(String testSuiteId)
    void putCurrentTestSuite(String testSuiteId, String testSuiteTimestamp)

    Path getBaseDir()
    Path getCurrentTestSuiteDirectory()
    Path getTestCaseDirectory(String testCaseId)

    Path resolveMaterial(String testCaseId, String url, FileType fileType)
    Path resolveMaterial(String testCaseId, String url, String suffix, FileType fileType)

    Path makeIndex() throws IOException
}
