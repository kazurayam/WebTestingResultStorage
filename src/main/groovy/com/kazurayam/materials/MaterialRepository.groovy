package com.kazurayam.materials

import java.nio.file.Path

/**
 * MaterialRepository#resolveMaterial() method resolves Path to save your 'Material'
 * obtained during a run of WebDriver-based testing.
 *
 * 'Material' includes for example
 * - PNG files as screenshots of a Web page
 * - PDF files downloaded from a Web page
 * - JSON files responded by Rest API
 * - XML files responded by SOAP server
 *
 * The file name of a Material is defined as:
 * <pre>
 * &lt;encoded URL&gt;§&lt;suffix&gt;.&lt;file extension&gt;
 * </pre>
 *
 * for example
 * <pre>
 * http%3A%2F%2Fdemoaut.katalon.com%2F§atoz.png
 * </pre>
 *
 * @author kazurayam
 *
 */
interface MaterialRepository {

    void putCurrentTestSuite(String testSuiteId)
    void putCurrentTestSuite(TSuiteName tSuiteName)
    void putCurrentTestSuite(String testSuiteId, String testSuiteTimestamp)
    void putCurrentTestSuite(TSuiteName tSuiteName, TSuiteTimestamp tSuiteTimestamp)

    String getCurrentTestSuiteId()
    String getCurrentTestSuiteTimestamp()

    Path getBaseDir()
    Path getCurrentTestSuiteDirectory()

    Path getTestCaseDirectory(String testCaseId)

    /**
     *
     * @param testCaseId e.g., 'Test Cases/TC1'
     * @param url e.g., 'http://demoaut.katalon.com/'
     * @return
     */
    Path resolveScreenshotPath(String testCaseId, URL url)
    Path resolveScreenshotPath(TCaseName tCaseName, URL url)

    Path resolveScreenshotPath(String testCaseId, Path subpath, URL url)
    Path resolveScreenshotPath(TCaseName tCaseName, Path subpath, URL url)

    /**
     *
     * @param testCaseId
     * @param fileName
     * @return
     */
    Path resolveMaterialPath(String testCaseId, String fileName)
    Path resolveMaterialPath(TCaseName testCaseName, String fileName)

    /**
     *
     * @param testCaseId e.g., 'Test Cases/TC1'
     * @param subpath '.', 'foo' or 'foo/bar'
     * @param fileName 'myfile.xls'
     * @return
     */
    Path resolveMaterialPath(String testCaseId, Path subpath, String fileName)
    Path resolveMaterialPath(TCaseName testCaseName, Path subpath, String fileName)

    /**
     *
     * @param first
     * @param more
     * @return
     */
    int deleteFilesInDownloadsDir(String fileName)

    /**
     *
     * @param testCaseId e.g., 'Test Cases/TC1'
     * @param fileName e.g., 'smilechart.xls'
     * @return
     */
    Path importFileFromDownloadsDir(String testCaseId, String fileName)
    Path importFileFromDownloadsDir(TCaseName tCaseName, String fileName)


    /**
     * Scan the <pre>[project dir]/Materials</pre> directory to create <pre>[project dir]/Materials/index.html</pre> file.
     * @return
     */
    Path makeIndex()


    /**
     * ImageDiff calls this
     *
     * @param tSuiteName
     * @param expectedProfile
     * @param actualProfile
     * @return
     */
    List<MaterialPair> createMaterialPairs(
        TSuiteName tSuiteName, ExecutionProfile expectedProfile, ExecutionProfile actualProfile)

    /**
     *
     * @param directory delete descendant directories and files of the specified directory. The directory is retained.
     */
    void deleteBaseDirContents() throws IOException
}
