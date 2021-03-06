package com.kazurayam.materials.metadata

import com.kazurayam.materials.*
import com.kazurayam.materials.metadata.InvokedMethodName
import com.kazurayam.materials.metadata.MaterialMetadata
import com.kazurayam.materials.metadata.MaterialMetadataBundle
import com.kazurayam.materials.metadata.MaterialMetadataImpl
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MaterialMetadataBundleSpec extends Specification {
    
    static Logger logger_ = LoggerFactory.getLogger(MaterialMetadataBundleSpec.class)
    
    // fields
    private static Path fixtureDir_ = Paths.get("./src/test/fixture")
    private static Path specOutputDir_
    
    // fixture methods
    def setupSpec() {
        specOutputDir_ = Paths.get("./build/tmp/testOutput/${Helpers.getClassShortName(MaterialMetadataBundleSpec.class)}")
        if (! Files.exists(specOutputDir_)) {
            Files.createDirectories(specOutputDir_)
        }
    }
    def setup() {}
    def cleanup() {}
    def cleanupSpec() {}

    // feature methods
    def testSmoke() {
        setup:
        Path caseOutputDir = specOutputDir_.resolve('testSmoke')
        Path materials = caseOutputDir.resolve('Materials')
        Path monitor47NewsDir = materials.resolve('Monitor47News')
        Files.createDirectories(monitor47NewsDir)
        Helpers.copyDirectory(
            fixtureDir_.resolve('Materials').resolve('Monitor47News'),
            monitor47NewsDir)
        when:
        TCaseName tCaseName = new TCaseName('Test Cases/main/TC1')
        String materialPath = 
            materials.resolve('Monitor47News')
                .resolve('20190123_153854')
                    .resolve(tCaseName.getValue())
                        .resolve('47NEWS_TOP.png')
                            .normalize().toString()
        MaterialDescription description = new MaterialDescription("category text", "description text")
        MaterialMetadata metadata = new MaterialMetadataImpl(
                InvokedMethodName.RESOLVE_SCREENSHOT_PATH_BY_URL_PATH_COMPONENTS,
                tCaseName,
                materialPath,
                description)
        //
        metadata.setSubPath('')
        URL url = new URL('https://www.47news.jp/47NEWS_TOP.png')
        metadata.setUrl(url)
        then:
        metadata.getUrl() == url
        when:
        MaterialMetadataBundle mmb = new MaterialMetadataBundle()
        mmb.add(metadata)
        then:
        mmb.size() == 1
        when:
        List<MaterialMetadata> list = mmb.findByMaterialPath(materialPath)
        then:
        list.size()== 1
        when:
        MaterialMetadata lastMm = mmb.findLastByMaterialPath(materialPath)
        then:
        lastMm != null
        lastMm.getMaterialPath() == materialPath
        lastMm.getUrl()== url
    }
    
    def testSerializeAndDeserialize() {
        setup:
        Path caseOutputDir = specOutputDir_.resolve('testSerializeAndDeserialize')
        Path reports = caseOutputDir.resolve('Reports')
        Files.createDirectories(reports)
        Path materials = caseOutputDir.resolve('Materials')
        Path monitor47NewsDir = materials.resolve('Monitor47News')
        Files.createDirectories(monitor47NewsDir)
        Helpers.copyDirectory(
            fixtureDir_.resolve('Materials').resolve('Monitor47News'),
            monitor47NewsDir)
        MaterialRepository mr = MaterialRepositoryFactory.createInstance(materials)
        when:
        TSuiteResult tsr = mr.getTSuiteResult(TSuiteResultId.newInstance(
                new TSuiteName('Monitor47News'),
                new TExecutionProfile("default"),
                new TSuiteTimestamp('20190123_153854')))
        TCaseResult tcr = tsr.getTCaseResult(new TCaseName('Test Cases/main/visit47NEWS'))
        assert tcr != null
        assert tcr.getMaterialList().size() > 0
        Material mate = tcr.getMaterialList().get(0)
        MaterialDescription description = new MaterialDescription("category text", "description text")
        MaterialMetadata metadata = new MaterialMetadataImpl(
                InvokedMethodName.RESOLVE_SCREENSHOT_PATH_BY_URL_PATH_COMPONENTS,
                tcr.getTCaseName(),
                mate.getHrefRelativeToRepositoryRoot(),
                description)
        //
        metadata.setSubPath('')
        URL url = new URL('https://www.47news.jp/47NEWS_TOP.png')
        metadata.setUrl(url)
        //
        MaterialMetadataBundle mmb = new MaterialMetadataBundle()
        mmb.add(metadata)
        //
        Path serialized = tsr.getTSuiteTimestampDirectory().resolve(MaterialMetadataBundle.SERIALIZED_FILE_NAME)
        OutputStream os = new FileOutputStream(serialized.toFile())
        Writer writer = new OutputStreamWriter(os, "UTF-8")
        mmb.serialize(writer)
        then:
        Files.exists(serialized)
        when:
        MaterialMetadataBundle deserialized = MaterialMetadataBundle.deserialize(serialized)
        then:
        deserialized != null
        when:
        // now we will check the content of the deserialized 'path-resolution-log-bundle.json'
        // we will be specifically interested in the MaterialPath property to be relative to the baseDir of MaterialRepository instance
        Path serialized2 = tsr.getTSuiteTimestampDirectory().resolve('serialized2.json')
        OutputStream os2 = new FileOutputStream(serialized2.toFile())
        Writer writer2 = new OutputStreamWriter(os2, "UTF-8")
        mmb.serialize(writer2)
        then:
        Files.exists(serialized2)
        when:  // the MaterialPath is expected to be relative to the baseDir of MaterialRepository, = the 'Materials' directory
        def jsonObject = new JsonSlurper().parse(serialized2.toFile())
        String materialPath = jsonObject.MaterialMetadataBundle[0].MaterialMetadata.MaterialPath
        then:
        // should not be "build\\tmp\\testOutput\\MaterialMetadataBundleSpec\\testSerializeAndDeserialize\\Materials\\Monitor47News\\20190123_153854\\main.TC1\\47NEWS_TOP.png"
        // should rather be "Monitor47News\\20190123_153854\\main.TC1\\47NEWS_TOP.png"
        materialPath.startsWith('Monitor47News')
    }

    private Tuple2<MaterialRepository, MaterialMetadataBundle> setupCaseOutput(Path caseOutputDir) {
        Path reports = caseOutputDir.resolve('Reports')
        Files.createDirectories(reports)
        Path materials = caseOutputDir.resolve('Materials')
        Path monitor47NewsDir = materials.resolve('Monitor47News')
        Files.createDirectories(monitor47NewsDir)
        Helpers.copyDirectory(
                fixtureDir_.resolve('Materials').resolve('Monitor47News'),
                monitor47NewsDir)
        MaterialRepository mr = MaterialRepositoryFactory.createInstance(materials)
        TSuiteResult tsr = mr.getTSuiteResult(TSuiteResultId.newInstance(
                new TSuiteName('Monitor47News'),
                new TExecutionProfile("default"),
                new TSuiteTimestamp('20190123_153854')))
        TCaseResult tcr = tsr.getTCaseResult(new TCaseName('Test Cases/main/visit47NEWS'))
        assert tcr != null
        assert tcr.getMaterialList().size() > 0
        Material mate = tcr.getMaterialList().get(0)
        MaterialDescription description = new MaterialDescription("category text", "description text")
        MaterialMetadata metadata = new MaterialMetadataImpl(
                InvokedMethodName.RESOLVE_SCREENSHOT_PATH_BY_URL_PATH_COMPONENTS,
                tcr.getTCaseName(),
                mate.getHrefRelativeToRepositoryRoot(),
                description)
        //
        metadata.setSubPath('')
        URL url = new URL('https://www.47news.jp/47NEWS_TOP.png')
        metadata.setUrl(url)
        //
        MaterialMetadataBundle mmb = new MaterialMetadataBundle()
        mmb.add(metadata)

        return new Tuple2(mr, mmb)
    }

    def test_serializeAsMarkdown() {
        setup:
        Path caseOutputDir = specOutputDir_.resolve('test_serializeAsMarkdown')
        Tuple2<MaterialRepository, MaterialMetadataBundle> tuple = this.setupCaseOutput(caseOutputDir)
        MaterialRepository mr = tuple[0]
        MaterialMetadataBundle mmb = tuple[1]
        when:
        File markdown = mr.getBaseDir().resolve(MaterialMetadataBundle.URLS_MARKDOWN_FILE_NAME).toFile()
        OutputStream os = new FileOutputStream(markdown)
        Writer writer = new OutputStreamWriter(os, "UTF-8")
        mmb.serializeAsMarkdown(writer)
        then:
        markdown.exists()
        when:
        String content = markdown.text
        then:
        content.contains("### category text")
        content.contains("| description text |")
    }

    def test_serializeAsTSV() {
        setup:
        Path caseOutputDir = specOutputDir_.resolve('test_serializeAsTSV')
        Tuple2<MaterialRepository, MaterialMetadataBundle> tuple = this.setupCaseOutput(caseOutputDir)
        MaterialRepository mr = tuple[0]
        MaterialMetadataBundle mmb = tuple[1]
        when:
        File tsv = mr.getBaseDir().resolve(MaterialMetadataBundle.URLS_TSV_FILE_NAME).toFile()
        OutputStream os = new FileOutputStream(tsv)
        Writer writer = new OutputStreamWriter(os, "UTF-8")
        mmb.serializeAsTSV(writer)
        then:
        tsv.exists()
        when:
        String content = tsv.text
        then:
        content.contains("### category text")
        content.contains("description text\t")
    }

}
