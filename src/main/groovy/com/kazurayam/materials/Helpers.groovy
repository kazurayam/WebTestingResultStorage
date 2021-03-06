package com.kazurayam.materials

import groovy.json.StringEscapeUtils
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemLoopException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.List
import java.util.stream.Collectors

import static java.nio.file.FileVisitResult.CONTINUE
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

final class Helpers {

    static Logger logger_ = LoggerFactory.getLogger(Helpers.class)

    /**
     * Constructor is hidden as this class is not supposed to be instanciated
     */
    private Helpers() {}

    /**
     * utility method which stringifies a java.time.LocalDateTime object
     * into a String of 'yyyyMMdd_HHmmss' format
     *
     * @param timestamp
     * @return string in 'yyyyMMdd_HHmmss'
     */
    static String getTimestampAsString(LocalDateTime timestamp) {
        return DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmss")
                .format(timestamp)
    }

    /**
     * utility method which creates the directory and its ancestors if not present
     *
     * @param directory
     */
    static void ensureDirs(Path directory) {
        if (!(Files.exists(directory))) {
            try {
                Files.createDirectories(directory)
            }
            catch (IOException e) {
                System.err.println(e)
            }
        }
    }

    /**
     * force-delete the directory and its contents(files and directories).
     * Will say again, the specified directory will be removed!
     * If you like to retain the directory, you should use deleteDirectoryContents(Path) method instead.
     * If the specified directory does not exit or is not a directory, then Exception will be thrown
     *
     * @param directoryToBeDeleted
     * @return number of files deleted, excluding directories deleted
     */
    static int deleteDirectory(Path directory) throws IOException {
        Objects.requireNonNull(directory, 'directory must not be null')
        if (!Files.exists(directory)) {
            logger_.warn("${directory.normalize().toAbsolutePath()} does not exist")
            return 0
        }
        if (!Files.isDirectory(directory)) {
            logger_.warn("${directory.normalize().toAbsolutePath()} is not a directory")
            return 0
        }
        int count = 0
        Files.walkFileTree(directory,
            new HashSet<>(),
            Integer.MAX_VALUE,
            new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                    if (exception == null) {
                        logger_.debug("#deleteDirectory deleting directory ${dir.toString()}")
                        //try {
                        //    Files.delete(dir)
                        //} catch (DirectoryNotEmptyException e) {
                        //    throw new IOException("Failed to delete ${dir} because it is not empty", e)
                        //}
                        boolean result = FileUtils.deleteQuietly(dir.toFile())
                        if (!result) {
                            logger_.warn("#deleteDirectory problem occured when deleting ${dir} quietly")
                        }
                        //return checkNotExist(dir)
                    }
                    return CONTINUE
                }
                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                    logger_.debug("#deleteDirectory deleting file      ${file.toString()}")
                    Files.delete(file)
                    count += 1
                    return CONTINUE
                }
                private FileVisitResult checkNotExist(final Path path) throws IOException {
                    if (!Files.exists(path)) {
                        return CONTINUE
                    } else {
                        throw new IOException()
                    }
                }
                @Override
                FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if (exc instanceof FileSystemLoopException) {
                        //logger_.warn("circular link was detected: " + file)
                        System.err.println("[RepositoryFileVisitor#visitFileFailed] circular link was detected: "
                            + file + ", which will be ignored")
                    } else {
                        //logger_.warn("unable to process file: " + file)
                        System.err.println("[RepositoryFileVisitor#visitFileFailed] unable to process file: "
                            + file + ", which will be ignored")
                    }
                    return CONTINUE
                }
            }
        )
        return count
    }
    
    private static FileVisitResult checkNotExist(final Path path) throws IOException {
        if (! Files.exists(path)) {
            return 
        } else {
            throw new IOException("${path} remains")
        }
    }


    /**
     * If the specified directory exists, then delete contained files and child directories
     * while preserving the directory undeleted.
     *
     * If the specified directory does not exit, silently returns while doing nothing.
     *
     * @return
     */
    static void deleteDirectoryContents(Path directory) throws IOException {
        if (Files.exists(directory)) {
           List<Path> children = Files.list(directory).collect(Collectors.toList());
           for (Path child : children) {
               if (Files.isRegularFile(child)) {
                   Files.delete(child)
               } else if (Files.isDirectory(child)) {
                   deleteDirectory(child)
               } else {
                   logger_.warn("#deleteDirectoryContents ${child.toString()} " +
                       "is not a File nor a Directory")
               }
            }
        }
    }


    /**
     * Check if a file is present or not. 
     * If not present, create the file of 0 bytes at the specified Path with the current timestamp.
     * This simulate UNIX touch command for a Path
     *
     * @param filePath
     * @throws IOException
     */
    static void touch(Path filePath) throws IOException {
        if (Files.notExists(filePath)) {
            filePath.toFile().createNewFile()
            filePath.toFile().setLastModified(System.currentTimeMillis())
        }
    }

    /**
     * Copies descendent files and directories recursively
     * from the source directory into the target directory.
     *
     * @param source a directory from which files and directories are copied
     * @param target a directory into which files and directories are copied
     * @param skipExisting default to true
     * @return
     */
    static int copyDirectory(Path source, Path target, boolean skipIfIdentical = true) {
        if (source == null) {
            throw new IllegalArgumentException('source is null')
        }
        if (!Files.exists(source)) {
            throw new FileNotFoundException("${source.normalize().toAbsolutePath()} does not exist")
        }
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("${source.normalize().toAbsolutePath()} is not a directory")
        }
        if (!Files.isReadable(source)) {
            throw new IllegalArgumentException("${source.normalize().toAbsolutePath()} is not readable")
        }
        if (target == null) {
            throw new IllegalArgumentException('target is null')
        }
        
        // if target directory is not there, create it
        Files.createDirectories(target)
        
        // number of files copied
        int count = 0
        
        Files.walkFileTree(source, 
            new HashSet<>(),
            Integer.MAX_VALUE,
            new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
                    Path targetdir = target.resolve(source.relativize(dir))
                    try {
                        Files.copy(dir, targetdir)
                    } catch (FileAlreadyExistsException e) {
                        if (!Files.isDirectory(targetdir))
                            throw e
                    }
                    return CONTINUE
                }
                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                    Path targetFile = target.resolve(source.relativize(file))
                    File sourceF = file.toFile()
                    File targetF = targetFile.toFile()
                    if (skipIfIdentical &&
                        Files.exists(targetFile) && 
                        sourceF.length() == targetF.length() &&
                        sourceF.lastModified() == targetF.lastModified()) {
                        ; // skip copying if sourceF and targetF are identical
                    } else {
                        logger_.debug("#copyDirectory copied ${file} to ${targetFile}")
                        Files.copy(file, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES)
                        count += 1
                    }
                    return CONTINUE
                }
                @Override
                FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if (exc instanceof FileSystemLoopException) {
                        //logger_.warn("circular link was detected: " + file)
                        System.err.println("[RepositoryFileVisitor#visitFileFailed] circular link was detected: "
                            + file + ", which will be ignored")
                    } else {
                        //logger_.warn("unable to process file: " + file)
                        System.err.println("[RepositoryFileVisitor#visitFileFailed] unable to process file: "
                            + file + ", which will be ignored")
                    }
                    return CONTINUE
                }
            }
        )
        
        return count
    }

    /**
     * returns a short name of the Class stripping the package.
     * For example, if
     * <pre>com.kazurayam.ksbackyard.screenshotsupport.ScreenshotRespsitoryImpl</pre> is given as
     * <pre>clazz</pre>, then <pre>ScreenshotRepositoryImpl</pre> is returned.
     *
     * @param clazz
     * @return
     */
    static String getClassShortName(Class clazz) {
        String fqdn = clazz.getName()
        String packageStr = clazz.getPackage().getName()
        String shortName = fqdn.replaceFirst(packageStr + '.', '')
        return shortName
    }


    /**
     * This method converts a Java string into a JSON string.
     * This method is implemented with groovy.json.StringEscapeUtils#escapeJava(String)
     *
     * @see <a href="http://docs.groovy-lang.org/latest/html/gapi/groovy/json/StringEscapeUtils.html#StringEscapeUtils()">escapeJava(String)</a>
     * @param string
     * @return
     */
    static String escapeAsJsonText(String string) {
        return StringEscapeUtils.escapeJava(string)
    }

    /**
     * returns the current time stamp in the format of 'yyyyMMdd_HHmmss'
     *
     * @return e.g., '20180616_070237'
     */
    static String now() {
        return DateTimeFormatter.ofPattern(TSuiteTimestamp.DATE_TIME_PATTERN).format(LocalDateTime.now())
    }

    /**
     * creates a BufferedImage that shows the given text
     * 
     * see http://burnignorance.com/java-web-development-tips/converting-text-to-image-in-java-awt/
     */
    static BufferedImage convertTextToImage(String text) {
        Objects.requireNonNull(text, "text must not be null")
        
        // create the font I wish to use
        Font font = new Font("Arial", Font.PLAIN, 20)
        
        // background-color
        Color LIGHT_YELLOW = new Color(255,255,153)
        Color GOLD = new Color(255,204,51)
        
        // create the FontRenderContext object which helps us to measure the text
        FontRenderContext frc = new FontRenderContext(
            null /*AffinTransform*/,
            RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT /*aaHint*/,
            RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT /*fmHint*/)
        
        // get the height and widht of the text
        Rectangle2D bounds = font.getStringBounds(text, frc)
        int w = (int)bounds.getWidth() + 6
        int h = (int)bounds.getHeight() + 3
        
        // create a BufferedImage object
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        
        // calling createGraphics() to get the Graphics2D
        Graphics2D g = image.createGraphics()
        g.setColor(GOLD)
        g.fillRect(0, 0, w, h)
        g.setColor(Color.BLACK)
        g.setFont(font)
        
        g.drawString(text, (float)bounds.getX(), (float) -bounds.getY())

        // releasing resources
        g.dispose()
        
        return image
    }
    
    /**
     * Generate a BufferedImage that shows a long path of a file in tree format so that the image has width as small as possible
     * 
     * @param multiLineText e.g, 
     * """
     * File not found:
     * C:
     * └─Users
     *   └─qcq0264
     *     └─eclipse-workspace
     *       └─Materials
     *         └─src
     *           └─main
     *             └─groovy
     *               └─com
     *                 └─kazurayam
     *                   └─materials
     *                     └─Helpers.groovy
     * """
     * @return a BufferedImage which shows the above text
     */
    static BufferedImage convertMultiLineTextToImage(String multiLineText) {
        Objects.requireNonNull(multiLineText, "multiLineText must not be null")
        BufferedReader br = new BufferedReader(new StringReader(multiLineText))
        List<String> lines = new ArrayList<String>()
        String line
        while ((line = br.readLine()) != null) {
            lines.add(line)
        }
        br.close()
        return convertMultiLineTextToImage(lines)
    }
    
    static BufferedImage convertMultiLineTextToImage(List<String> lines) {
        Objects.requireNonNull(lines, "lines must not be null")
        //println "lines: ${lines}"
        int MIN_IMAGE_WIDTH = 800
        int MIN_IMAGE_HEIGHT = 200
        // create the font I wish to use
        Font font = new Font("Courier", Font.PLAIN, 20)
        // create the FontRenderContext object which helps us to measure the text
        FontRenderContext frc = new FontRenderContext(
            null /*AffinTransform*/,
            RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT /*aaHint*/,
            RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT /*fmHint*/)
        // find the longest line to resolve the width of image
        int imageWidth = 0
        for (String line : lines) {
            Rectangle2D bounds = font.getStringBounds(line, frc)
            int w = (int)bounds.getWidth()
            imageWidth = (w > imageWidth) ? w : imageWidth
        }
        imageWidth = (imageWidth > MIN_IMAGE_WIDTH) ? imageWidth : MIN_IMAGE_WIDTH
        // accumulate the line height of the lines to resolve the height of image
        int lineHeight = 0
        int imageHeight = 0
        for (String line : lines) {
            Rectangle2D bounds = font.getStringBounds(line, frc)
            int h = (int)bounds.getHeight() + 3
            imageHeight += h
            lineHeight = h
        }
        imageHeight = (imageHeight > MIN_IMAGE_HEIGHT) ? imageHeight : MIN_IMAGE_HEIGHT
        // create a BufferedImage object
        Color GOLD = new Color(255,204,51)
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)
        Graphics2D g = image.createGraphics()
        int c = 1
        for (String line : lines) {
            //println "drawing ${line}"
            Rectangle2D bounds = font.getStringBounds(line, frc)
            g.setColor(GOLD)
            g.fillRect(0, lineHeight * (c - 1), imageWidth, lineHeight)
            g.setColor(Color.BLACK)
            g.setFont(font)
            g.drawString(line, (float)bounds.getX(), (float) -(bounds.getY() * c))
            c += 1
        }
        g.dispose()
        
        return image
    }
    
    /**
     * Given a long Path, return an array of String which shows the give path in tree format.
     * 
     * @param Path e.g, "C:\Users\myname\eclipse-workspace\Materials\src\main\groovy\com\kazurayam\materials\Helpers.groovy"
     * @return List<String>; indented with 2 white space, with branch mark └─.  e.g,
     * """
     * C:
     * └─Users
     *   └─qcq0264
     *     └─eclipse-workspace
     *       └─Materials
     *         └─src
     *           └─main
     *             └─groovy
     *               └─com
     *                 └─kazurayam
     *                   └─materials
     *                     └─Helpers.groovy
     * """
     */
    static List<String> toTreeFormat(Path path) {
        String BRANCH = '└─'
        String INDENT = '  '
        List<String> list = new ArrayList<String>()
        if (path.getRoot() != null) {
            list.add(path.getRoot().toString())
        }
        int depth = 1
        Iterator iter = path.iterator()
        while (iter.hasNext()) {
            Path e = iter.next()
            StringBuilder sb = new StringBuilder()
            if (depth > 0) {
                sb.append(INDENT * depth)
                sb.append(BRANCH)
            }
            sb.append(e.toString())
            list.add(sb.toString())
            depth += 1
        }
        return list
    }
    
}
