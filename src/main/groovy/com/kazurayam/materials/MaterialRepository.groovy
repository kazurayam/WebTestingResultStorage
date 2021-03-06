package com.kazurayam.materials

import com.kazurayam.materials.metadata.MaterialMetadataBundle

import java.nio.file.Path

import com.kazurayam.materials.repository.RepositoryRoot

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
interface MaterialRepository extends TSuiteResultTree {

    /**
     * scan the baseDir to recognize the current directories/files configuration
     */
    void scan()


    /**
     * delete all descendant directories and files belonging to the tSuiteName + tSuiteTimestamp directory.
     * will remove the tSuiteTimestamp directory, but will retain the tSuiteName directory.
     *
     * @param tSuiteResultId
     * @return number of material files deleted. number of directories are not included.
     * @throws IOException
     */
    int clear(List<TSuiteResultId> tSuiteResultIdList) throws IOException

    /**
     * delete all descendant directories and files beloging to the tSuiteName directory.
     * will remove the tSuiteTimestamp directory.
     *
     * @param tSuiteName
     * @return number of material files deleted. number of directories are not included.
     * @throws IOException
     */
    int clear(TSuiteName tSuiteName) throws IOException

    /**
     * delete all descendant directories and files belonging to the tSuiteName + tSuiteTimestamp directory.
     * will remove the tSuiteTimestamp directory, but will retain the tSuiteName directory.
     *
     * @param tSuiteResultId
     * @param to perform scan() or not
     * @return number of material files deleted. number of directories are not included.
     * @throws IOException
     */
    int clear(TSuiteResultId tSuiteResultId, boolean scan) throws IOException

    /**
     * identify all TSuiteResultId contained in the MaterialRespository,
     * and delete those except the specfied one. For example, you can identify
     * the TSuiteResultId which is marked as current, and delete the rest except
     * the current one by this:
     *
     * <PRE>
     * MaterialRepository mr = ...
     * TSuiteResultId currentTSuiteResultId = mr.getCurrentTSuiteResult().getId()
     * mr.clearRest(currentTSuiteResultId, true)
     * </PRE>
     *
     * @param tSuiteResultId
     * @param Scan
     * @return
     * @throws IOException
     */
    int clearRest(TSuiteResultId tSuiteResultId, boolean scan) throws IOException


    /**
     * Scans the Materials directory to look up pairs of Material objects to compare
     * for the Twins mode.
     *
     * This method perform the following search under the Materials directory
     * in order to identify which Material object to be included.
     *
     * 1. selects all ./Materials/<tSuiteName>/<ExecutionProfile>/yyyyMMdd_hhmmss directories
     *    with specified tSuiteName. Twins-mode disregards the Execution Profile.
     * 2. among them, select the directory with the 1st latest timestamp.
     *    This one is regarded as "Actual one".
     * 3. among them, select the directory with the 2nd latest timestamp.
     *    This one is regarded as "Expected one".
     * 4. Scan the 2 directories chosen. Create a List of Material objects.
     *    Two files which have the same path
     *    under the yyyyMMdd_hhmmss directory will be packaged as a pair to form a MaterialPair object.
     *
     * @return List<MaterialPair>
     */
    MaterialPairs createMaterialPairsForTwinsMode(TSuiteName capturingTSuiteName)

    /**
     * Scans the Materials directory to look up pairs of Material objects to compare
     * for the Chronos mode
     *
     * This method perform the following search under the Materials directory
     * in order to identify which Material object to be included.
     *
     * 1. selects all ./Materials/<tSuiteName>/<ExecutionProfile>/yyyyMMdd_hhmmss directories
     *    with specified tSuiteName and Execution Profile. For the Chronos mode,
     *    the applied Execution Profile matters.
     * 2. among them, select the directory with the 1st latest timestamp.
     *    This one is regarded as "Actual one".
     * 3. among them, select the directory with the 2nd latest timestamp.
     *    This one is regarded as "Expected one".
     * 4. Scan the 2 directories chosen. Create a List of Material objects.
     *    Two files which have the same path under the yyyyMMdd_hhmmss directory
     *    will be packaged as a pair to form a MaterialPair object.
     *
     * @return List<MaterialPair>
     */
    MaterialPairs createMaterialPairsForChronosMode(TSuiteName capturingTSuiteName,
                                      TExecutionProfile capturingTExecutionProfile)

    /**
     *
     * delete all descendant directories and files of the base directory. The base directory is retained.
     */
    void deleteBaseDirContents() throws IOException

    Path getBaseDir()
    
    Path getCurrentTestSuiteDirectory()

    String getCurrentTestSuiteId()
    String getCurrentExecutionProfile()
    String getCurrentTestSuiteTimestamp()

    TSuiteResult getCurrentTSuiteResult()

    long getSize()
    

	RepositoryRoot getRepositoryRoot()
    
    Set<Path> getSetOfMaterialPathRelativeToTSuiteTimestamp(TSuiteName tSuiteName,
                                                            TExecutionProfile tExecutionProfile)
    Path getTestCaseDirectory(String testCaseId)
    
    /**
     * 
     * @return a TCaseResult object with tCaseName inside
     * the tSuiteName + tExecutionProfile + tSuiteTimestamp directory.
     * Returns null if not found.
     */
    TCaseResult getTCaseResult(TSuiteName tSuiteName,
                               TExecutionProfile tExecutionProfile,
                               TSuiteTimestamp tSuiteTimestamp,
                               TCaseName tCaseName)
    
    List<TSuiteName> getTSuiteNameList()
    TSuiteResult getTSuiteResult(TSuiteResultId tSuiteResultId)

    List<TSuiteResultId> getTSuiteResultIdList(TSuiteName tSuiteName, TExecutionProfile tExecutionProfile)
    List<TSuiteResultId> getTSuiteResultIdList()

    List<TSuiteResult> getTSuiteResultList(List<TSuiteResultId> tSuiteResultIdList)
    List<TSuiteResult> getTSuiteResultList()
    
    void markAsCurrent(String testSuiteId,
                       String executionProfile,
                       String testSuiteTimestamp)

    void markAsCurrent(TSuiteName tSuiteName,
                       TExecutionProfile tExecutionProfile,
                       TSuiteTimestamp tSuiteTimestamp)

    void markAsCurrent(TSuiteResultId tSuiteResultId)
    
    boolean isAlreadyMarked()

    /**
    * Scan the <pre>[project dir]/Materials</pre> directory to create <pre>[project dir]/Materials/index.html</pre> file.
    *
    * The makeIndex() method was once removed from the MaterialRepository interface at the Materials-0.71.0.jar.
    * See the diff at
    * https://github.com/kazurayam/Materials/commit/4e42834b4889949b93a4282c4a6e2d94f56795a6#diff-c7b1f72fa3889f9e5ae3ae79ec4d95f5
    *
    * But makeIndex() was added back at the Materials-0.71.4 in order to restore backward compatibility. See the related issue at
    * https://forum.katalon.com/t/logging-http-response-headers-and-bodies-of-web-service-materials-applied/13803/13
    *
    * @return the Path of index.html file
    */
    Path makeIndex()

    TSuiteResult ensureTSuiteResultPresent(String testSuiteName,
                                           String executionProfile,
                                           String testSuiteTimestamp)

    TSuiteResult ensureTSuiteResultPresent(TSuiteName tSuiteName,
                                           TExecutionProfile tExecutionProfile,
                                           TSuiteTimestamp tSuiteTimestamp)

    TSuiteResult ensureTSuiteResultPresent(TSuiteResultId tSuiteResultId)
    
    
    /**
     * Returns a Path of a Material file.
     * 
     * Examples:
     * <PRE>
     * MaterialRepository mr = MaterialRepositoryFactory.createInstance(Path.get('./Materials'))
     * mr.putCurrentTestSuite('Test Suites/main/TS1', '20180530_130419')
     * Path path = mr.resolveScreenshotPath(
     *                 'Test Cases/main/TC1',
     *                 new URL('https://katalon-demo-cura.herokuapp.com/'))
     * assert path.toString() == 'Materials/main.TS1/20180530_130419/main.TC1/http%3A%2F%2Fdemoaut.katalon.com%2F.png'
     * 
     * </PRE>
     */
    Path resolveScreenshotPath(String testCaseId,
                               URL url, MaterialDescription description)

    Path resolveScreenshotPath(String testCaseId, String subPath,
                               URL url, MaterialDescription description)

    Path resolveScreenshotPath(TCaseName tCaseName,
                               URL url, MaterialDescription description)

    Path resolveScreenshotPath(TCaseName tCaseName, String subPath,
                               URL url, MaterialDescription description)

    /**
     * Returns a Path of a Material file.
     * 
     * Examples:
     * <PRE>
     * MaterialRepository mr = MaterialRepositoryFactory.createInstance(Path.get('./Materials'))
     * mr.putCurrentTestSuite('Test Suites/main/TS1', '20180530_130419')
     * Path path = mr.resolveScreenshotPathByURLPathComponents(
     *                 'Test Cases/main/TC1',
     *                 new URL('https://katalon-demo-cura.herokuapp.com/'),
     *                 0,
     *                 'top')
     * assert path.toString().endsWith('Materials/main.TS1/20180530_130419/main.TC1/top.png')
     * </PRE>
     * 
     * <PRE>
     * MaterialRepository mr = MaterialRepositoryFactory.createInstance(Path.get('./Materials'))
     * mr.putCurrentTestSuite('Test Suites/main/TS1', '20180530_130419')
     * Path path = mr.resolveScreenshotPathByURLPathComponents(
     *                 'Test Cases/main/TC1',
     *                 new URL('https://katalon-demo-cura.herokuapp.com/aaa/appointment'),
     *                 1,
     *                 'top')
     * assert path.toString().endsWith('Materials/main.TS1/20180530_130419/main.TC1/appointment.png')
     * </PRE>
     * 
     */
    Path resolveScreenshotPathByURLPathComponents(String testCaseId, URL url,
                                                  int startingDepth, String defaultName, MaterialDescription description)

    Path resolveScreenshotPathByURLPathComponents(String testCaseId, String subPath, URL url,
                                                  int startingDepth, String defaultName, MaterialDescription description)

    Path resolveScreenshotPathByURLPathComponents(TCaseName tCaseName, URL url,
                                                  int startingDepth, String defaultName, MaterialDescription description)

    Path resolveScreenshotPathByURLPathComponents(TCaseName tCaseName, String subPath, URL url,
                                                  int startingDepth, String defaultName, MaterialDescription description)


    /**
     * Returns a Path of a Material file.
     * 
     * Example:
     * <PRE>
     * MaterialRepository mr = MaterialRepositoryFactory.createInstance(Path.get('./Materials'))
     * mr.putCurrentTestSuite('Test Suites/main/TS1', '20180530_130419')
     * Path path = mr_.resolveMaterialPath('Test Cases/main/TC1', 'screenshot1.png')
     * assert path.toString().endsWith('Materials/main.TS1/20180530_130419/main.TC1/screenshot1.png')
     * </PRE>
     */
    Path resolveMaterialPath(String testCaseId,
                             String fileName, MaterialDescription description)

    Path resolveMaterialPath(String testCaseId, String subPath,
                             String fileName, MaterialDescription description)

    Path resolveMaterialPath(TCaseName testCaseName,
                             String fileName, MaterialDescription description)

    Path resolveMaterialPath(TCaseName testCaseName, String subPath,
                             String fileName, MaterialDescription description)


    /**
     *
     * @param testCaseId
     * @param subPath
     * @param url
     * @param startingDepth
     * @param fileType
     * @return
     */
    Path resolveMaterialPathByURLPathComponents(String testCaseId,
                                                URL url, int startingDepth, String defaultName,
                                                FileType fileType, MaterialDescription description)

    Path resolveMaterialPathByURLPathComponents(String testCaseId, String subPath,
                                                URL url, int startingDepth, String defaultName,
                                                FileType fileType, MaterialDescription description)

    Path resolveMaterialPathByURLPathComponents(TCaseName testCaseName,
                                                URL url, int startingDepth, String defaultName,
                                                FileType fileType, MaterialDescription description)

    Path resolveMaterialPathByURLPathComponents(TCaseName testCaseName, String subPath,
                                                URL url, int startingDepth, String DefaultName,
                                                FileType fileType, MaterialDescription description)


    /**
     *
     * @param tSuiteResult
     * @return the path of <TSuiteResult>/material-metadata-bundle.json file
     */
    Path locateMaterialMetadataBundle(TSuiteResultId tSuiteResultId)
    Path locateMaterialMetadataBundle(TSuiteResult tSuiteResult)
	
	MaterialMetadataBundle findMaterialMetadataBundleOfCurrentTSuite()
	boolean hasMaterialMetadataBundleOfCurrentTSuite()

    boolean printVisitedURLsAsMarkdown(Writer writer)
    boolean printVisitedURLsAsTSV(Writer writer)


    String toJsonText()
}
