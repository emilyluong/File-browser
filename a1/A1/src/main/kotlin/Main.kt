import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.text.Text
import javafx.stage.Stage
import java.io.BufferedReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.io.path.isDirectory


class Main : Application() {

    fun countCharacter(word: String, charToCount: Char): Int {
        var count = 0
        for (ch in word) {
            if (ch == charToCount) {
                count++
            }
        }
        return count
    }

    fun promptUserFileRename(file: File): String {
        val textInputDialog = TextInputDialog(file.name)
        textInputDialog.headerText = "Enter a new filename/directory"
        val result = textInputDialog.showAndWait()

        if (result.isPresent) {
            // add extension for file rename if extension was not added by user
            // normal file with no extension x
            // normal file turn into hidden file no extension x
            // normal file turn into hidden file with extension
            // hidden file with no extension
            // hidden file turn into normal file no extension
            // hidden file turn into normal file with extension
            if (result.get() == "") {
                return ""
            }

            // hidden file name with no ext renames to another name, but still hidden
            // if file has no extension, there can be 2 ways:
            // 1. file is hidden -> new name is hidden
            // 2. file is hidden -> new name is not hidden
            // 3. file is not hidden -> new name is hidden
            // 4. file is not hidden -> new name is not hidden
            if (file.isFile &&
                (isHiddenFileOrDir(file.name) && countCharacter(file.name, '.') == 1 &&
                        ((isHiddenFileOrDir(result.get()) && countCharacter(result.get(), '.') == 1)
                                || !isHiddenFileOrDir(result.get()) && countCharacter(result.get(), '.') == 0))
                || (!isHiddenFileOrDir(file.name) && countCharacter(file.name, '.') == 0 &&
                        ((isHiddenFileOrDir(result.get()) && countCharacter(result.get(), '.') == 1)
                                || !isHiddenFileOrDir(result.get()) && countCharacter(result.get(), '.') == 0))) {
                return result.get()
            }

            if (file.isFile && (
                        (getFileExtension(result.get()) == "" && !isHiddenFileOrDir(file.name)) ||
                                (isHiddenFileOrDir(result.get()) && countCharacter(result.get(), '.') == 1 && !isHiddenFileOrDir(file.name)) ||
                                (!isHiddenFileOrDir(result.get()) && getFileExtension(result.get()) == "" &&  isHiddenFileOrDir(file.name)) ||
                                (isHiddenFileOrDir(result.get()) && countCharacter(result.get(), '.') == 1 && isHiddenFileOrDir(file.name)))) {
                return "${result.get()}.${getFileExtension(file.toPath().toString())}"
            }
            return result.get()
        } else {
            return file.name
        }
    }

    fun getCorrectFileOrDirName(path: File): String {
        return if (path.isDirectory) "${path.name}/" else path.name
    }

    fun getFileExtension(path: String): String {
        val ext = path.split('.').last()
        if (ext != path) {
            return ext
        }
        return ""
    }

    fun copyDir(src: Path, dest: Path) {
        val paths = Files.walk(src).toList()
        for (path in paths) {
            Files.copy(path, dest.resolve(src.relativize(path)),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    fun getFileNameMapToFullPath(path: File): HashMap<String, String> {
        val paths = Files.walk(path.toPath())
        val result = HashMap<String, String>()
        for (p in paths) {
            result[getCorrectFileOrDirName(p.toFile())] = p.toString()
        }
        return result
    }

    fun getFilePathMapToFileName(path: File): HashMap<String, String> {
        val paths = Files.walk(path.toPath())
        val result = HashMap<String, String>()
        for (p in paths) {
            result[p.toString()] = getCorrectFileOrDirName(p.toFile())
        }
        return result
    }

    fun setBottomPath(selectedFilePath: String): HBox {
        val label = Label(selectedFilePath)
        return HBox(label)
    }

    fun getUpdatedFileNameMapToFullPathForDir(selectedFilePath: String, newPathFile: File, fileNameMapToFullPath: HashMap<String, String>): HashMap<String, String> {
        val updatedFileNameMapToFullPath = fileNameMapToFullPath.filter { entry ->
            !entry.key.startsWith(selectedFilePath)
        }.toMutableMap()

        updatedFileNameMapToFullPath.putAll(getFileNameMapToFullPath(newPathFile))
        return updatedFileNameMapToFullPath as HashMap<String, String>
    }

    fun setCenterContent(selectedFilePath: String, border: BorderPane) {
        val extension = getFileExtension(selectedFilePath)

        if (extension == "png" || extension == "jpg" || extension == "bmp") {
            val fileURI = File(selectedFilePath).toURI().toString()
            val stackPane = StackPane()
            val bgImage = BackgroundImage(Image(fileURI), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize(0.0,
                0.0, false, false, true, false))

            stackPane.background = Background(bgImage)
            border.center = stackPane
        } else if (extension == "txt" || extension == "md") {
            val fileContent = TextArea()
            val bufferedReader: BufferedReader = File(selectedFilePath).bufferedReader()
            val fileText = bufferedReader.use { it.readText() }

            fileContent.isEditable = false
            fileContent.isWrapText = true
            fileContent.text = fileText

            border.center = fileContent
        } else if (File(selectedFilePath).isDirectory) {
            border.center = null
        } else if (!File(selectedFilePath).canRead()) {
            border.center = Text("File cannot be read")
        } else {
            border.center = Text("Unsupported type")
        }
    }

    fun getFileList(filePathList: List<File>, showHiddenFiles: Boolean): ObservableList<String> {
        var fileNames = FXCollections.observableArrayList<String>()
        for (file in filePathList) {
            var name = getCorrectFileOrDirName(file)
            // if start with . and showHiddenFiles is false, dont add to list
            if (isHiddenFileOrDir(name) && !showHiddenFiles) {
                continue
            }
            fileNames.add(name)
        }
        return fileNames
    }

    fun isHiddenFileOrDir(filename: String): Boolean {
        return filename.startsWith(".")
    }

    fun isSelectedFileTheFolder(selectedFilePath: String, fileList: ListView<String>): Boolean {
        val name = getCorrectFileOrDirName(File(selectedFilePath))
        return !fileList.items.contains(name)
    }

    fun getActualFileDirName(filenameView: String): String {
        return if (filenameView.endsWith("/")) filenameView.substringBefore("/") else filenameView
    }

    override fun start(stage: Stage) {
        // test directory
        val testDir = File("${System.getProperty("user.dir")}/test/")

        // init border
        val border = BorderPane()

        // init show hidden files
        var showHiddenFiles = false

        var selectedFilePath = testDir.path

        // get all file name to full path
        var filePathMapToFileName = getFilePathMapToFileName(testDir)

        // keeps track of file list that will be shown
        var fileList = ListView(getFileList(testDir.listFiles().toList(), showHiddenFiles))

        // function for filePathMapToFileName to get file path from file name of current dir
        fun getFilePathInCurrentDir(filename: String): String {
            val name = getActualFileDirName(filename)
            return if (isSelectedFileTheFolder(selectedFilePath, fileList)) {
                "$selectedFilePath/$name"
            } else {
                val selectedParentPath = File(selectedFilePath).parent
                "$selectedParentPath/$name"
            }
        }

        // init for click file handler
        fun getSelectedFilePathForClickHandler(it: MouseEvent): String {
            val target = it.target.toString()
            var targetFileName: String
            if (target.startsWith("ListViewSkin")) {
                targetFileName = target.substringAfter("'")
                targetFileName = targetFileName.substringBefore("'")
            } else {
                targetFileName = target.substringAfter("text=\"")
                targetFileName = targetFileName.substringBefore("\",")
            }

            // can occur when click on list without anything, hence just keep current selected file
            if (targetFileName == "null") {
                return selectedFilePath
            }
            return getFilePathInCurrentDir(targetFileName)
        }

        // event handler for using key event to go up and down the list files
        fun keyPressFileListHandler(it: KeyEvent) {
            fun fileClickHandler(): EventHandler<MouseEvent> = EventHandler {
                selectedFilePath = getSelectedFilePathForClickHandler(it)

                if (File(selectedFilePath).isDirectory && it.clickCount == 2) {
                    // Handle navigating into directory
                    fileList = ListView(getFileList(File(selectedFilePath).listFiles().toList(), showHiddenFiles))
                    fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                    fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
                    border.left = fileList
                } else {
                    if (selectedFilePath.startsWith('/')) {
                        setCenterContent(selectedFilePath, border)
                        // Change bottom path
                        border.bottom = setBottomPath(selectedFilePath)
                    }
                }
            }

            val currentSelectedFilePathIndex = fileList.items.indexOf(getCorrectFileOrDirName(File(selectedFilePath)))

            var nextSelectedFilePath = ""
            if (it.code == KeyCode.DOWN && currentSelectedFilePathIndex < fileList.items.size - 1) {
                val nextSelectedFileName = fileList.items[currentSelectedFilePathIndex + 1]
                if (!isSelectedFileTheFolder(selectedFilePath, fileList)) {
                    nextSelectedFilePath = "${File(selectedFilePath).parent}/${getActualFileDirName(nextSelectedFileName)}"
                }
            } else if (it.code == KeyCode.UP && currentSelectedFilePathIndex - 1 > -1) {
                val nextSelectedFileName = fileList.items[currentSelectedFilePathIndex - 1]
                if (!isSelectedFileTheFolder(selectedFilePath, fileList)) {
                    nextSelectedFilePath = "${File(selectedFilePath).parent}/${getActualFileDirName(nextSelectedFileName)}"
                }
            } else if (it.code == KeyCode.ENTER && File(getFilePathInCurrentDir(fileList.items[currentSelectedFilePathIndex])).isDirectory) {
                // helper for when ENTER is pressed on selected directory, want to descend into directory
                // Update the file list view to the directory selected
                fileList = ListView(getFileList(File(selectedFilePath).listFiles().toList(), showHiddenFiles))
                fileList.setOnKeyPressed { keyPressFileListHandler(it) }
                fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())

                border.left = fileList
            }


            if (nextSelectedFilePath != "") {
                border.bottom = setBottomPath(nextSelectedFilePath)
                selectedFilePath = nextSelectedFilePath
                setCenterContent(selectedFilePath, border)
            }
        }

        // event handler for file items --> VERY IMPORTANT WHEN CHANGING LIST VIEW
        fun fileClickHandler(): EventHandler<MouseEvent> = EventHandler {
            selectedFilePath = getSelectedFilePathForClickHandler(it)

            if (File(selectedFilePath).isDirectory && it.clickCount == 2) {
                // Handle navigating into directory
                fileList = ListView(getFileList(File(selectedFilePath).listFiles().toList(), showHiddenFiles))
                fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
                border.left = fileList
            } else {
                if (selectedFilePath.startsWith('/')) {
                    setCenterContent(selectedFilePath, border)
                    // Change bottom path
                    border.bottom = setBottomPath(selectedFilePath)
                }
            }
        }

        // event handler for using backspace/delete key event
        fun backspaceFileListHandler(it: KeyEvent) {
            var nextSelectedFilePath = ""

            // only allow backspace when path
            if ((it.code == KeyCode.DELETE || it.code == KeyCode.BACK_SPACE )
                && (selectedFilePath != testDir.path &&
                        (File(selectedFilePath).parent != testDir.path && (!File(selectedFilePath).isFile || (!File(selectedFilePath).isDirectory && !isSelectedFileTheFolder(selectedFilePath, fileList)))
                        || (File(selectedFilePath).parent == testDir.path && File(selectedFilePath).isDirectory && isSelectedFileTheFolder(selectedFilePath, fileList))))) {
                if (isSelectedFileTheFolder(selectedFilePath, fileList) && File(selectedFilePath).isDirectory) {
                    fileList = ListView(getFileList(File(selectedFilePath).parentFile.listFiles().toList(), showHiddenFiles))
                    nextSelectedFilePath = File(selectedFilePath).parentFile.path
                } else {
                    fileList = ListView(getFileList(File(selectedFilePath).parentFile.parentFile.listFiles().toList(), showHiddenFiles))
                    nextSelectedFilePath = File(selectedFilePath).parentFile.parentFile.path
                }
                fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
                border.left = fileList
            }

            if (nextSelectedFilePath != "") {
                border.bottom = setBottomPath(nextSelectedFilePath)
                selectedFilePath = nextSelectedFilePath
                setCenterContent(selectedFilePath, border)
            }
        }

        fun nameAlreadyExist(newName: String, oldFilePath: File): Boolean {
            return if (oldFilePath.isDirectory) {
                fileList.items.contains("$newName/")
            } else {
                fileList.items.contains(newName)
            }
        }

        // event handler for renaming files --> IMPORTANT FOR RENAME BUTTON AND RENAME MENU ITEM IN ACTION
        fun renameClickHandlerHelper() {
            if (selectedFilePath != testDir.path && !isSelectedFileTheFolder(selectedFilePath, fileList)) {
                val selectedFilePathFile = File(selectedFilePath)
                val newFileName = promptUserFileRename(selectedFilePathFile)

                if (newFileName == selectedFilePathFile.name) {
                    return
                }

                if (newFileName == ""
                    || newFileName.contains("/")
                    || nameAlreadyExist(newFileName, selectedFilePathFile)
                    || selectedFilePathFile.isDirectory && !isHiddenFileOrDir(newFileName) && countCharacter(newFileName, '.') > 0
                    || !isHiddenFileOrDir(newFileName) && countCharacter(newFileName, '.') > 1
                    || isHiddenFileOrDir(newFileName) && countCharacter(newFileName, '.') > 2
                    || isHiddenFileOrDir(newFileName) && newFileName.startsWith("..")) {
                    val errorPopup = Alert(Alert.AlertType.ERROR)
                    errorPopup.title = "Unable to Rename File/Directory"
                    errorPopup.headerText = "Invalid File/Directory Name"
                    errorPopup.contentText = "The name entered is invalid. The following can be the issue: \n\n" +
                            "- Empty file/Directory name; \n" +
                            "- File/Directory name already exists in this directory; \n" +
                            "- File/Directory name entered contains '/'; \n" +
                            "- Directory name entered contains a '.' in a position other than the start of the name (to indicate hidden); \n" +
                            "- File/Directory name entered contains more than 2 '.' for hidden file; \n" +
                            "- File/Directory name entered contains more than 1 '.' for non-hidden file. \n" +
                            "- File/Directory name entered has '..' in the start. \n"
                    errorPopup.showAndWait()
                    return
                }

                try {
                    val newPath = selectedFilePath.substringBeforeLast("/") + "/" + newFileName
                    val newPathFile = File(newPath)
                    val currentFileList = fileList.items.toList().toMutableList()
                    val indexOfChangedFile: Int

                    if (selectedFilePathFile.isDirectory) {
                        // If want to rename a file, then need to create new dir with new name, then copy all contents
                        // of the old dir path. Then delete contents of old dir path
                        newPathFile.mkdir()
                        copyDir(selectedFilePathFile.toPath(), newPathFile.toPath())
                        File(selectedFilePath).deleteRecursively()

                        // Update fileNameMapToFullPath
                        filePathMapToFileName = getUpdatedFileNameMapToFullPathForDir(selectedFilePath, newPathFile, filePathMapToFileName)

                        indexOfChangedFile = currentFileList.indexOf("${selectedFilePathFile.name}/")
                    } else {
                        filePathMapToFileName.remove(selectedFilePathFile.path)
                        filePathMapToFileName[newPath] = getCorrectFileOrDirName(newPathFile)
                        selectedFilePathFile.renameTo(newPathFile)
                        indexOfChangedFile = currentFileList.indexOf(selectedFilePathFile.name)
                    }

                    // Update fileList and selectedFilePath
                    currentFileList[indexOfChangedFile] = getCorrectFileOrDirName(newPathFile)

                    val updatedFilePath = ArrayList<File>()
                    for (file in currentFileList) {
                        updatedFilePath.add(File(getFilePathInCurrentDir(file)))

                    }
                    fileList = ListView(getFileList(updatedFilePath, showHiddenFiles))
                    fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                    fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
                    border.left = fileList

                    // if show hidden files is true, it is okay to show the hidden file. Otherwise, need to change selected file and content shown
                    if (!showHiddenFiles && isHiddenFileOrDir(getCorrectFileOrDirName(File(selectedFilePath)))) {
                        if (showHiddenFiles) {
                            border.bottom = setBottomPath(newPath)
                            selectedFilePath = newPath
                        } else {
                            val nonHiddenNewPath = newPath.substringBefore("/.")
                            border.bottom = setBottomPath(nonHiddenNewPath)
                            border.center = null
                            selectedFilePath = nonHiddenNewPath
                        }
                    } else {
                        border.bottom = setBottomPath(newPath)
                        selectedFilePath = newPath
                    }

                } catch (e: Exception) {
                    val errorPopup = Alert(Alert.AlertType.ERROR)
                    errorPopup.title = "Unable to Rename File/Directory"
                    errorPopup.headerText = "Invalid File/Directory Name"
                    errorPopup.contentText = "The name entered is invalid. The following can be the issue: \n\n" +
                            "- Empty file/Directory name; \n" +
                            "- File/Directory name already exists in this directory; \n" +
                            "- File/Directory name entered contains '/'; \n" +
                            "- Directory name entered contains a '.' in a position other than the start of the name (to indicate hidden); \n" +
                            "- File/Directory name entered contains more than 2 '.' for hidden file; \n" +
                            "- File/Directory name entered contains more than 1 '.' for non-hidden file. \n" +
                            "- File/Directory name entered has '..' in the start. \n"
                    errorPopup.showAndWait()
                }
            }
        }

        fun renameClickHandler(): EventHandler<MouseEvent> = EventHandler {
            renameClickHandlerHelper()
        }

        // event handler for moving files
        fun moveClickHandlerHelper() {
            if (selectedFilePath != testDir.path && !isSelectedFileTheFolder(selectedFilePath, fileList)) {
                try {
                    val selectedFilePathFile = File(selectedFilePath)
                    var dirList = Files.walk(testDir.toPath()).filter { path -> path.isDirectory() }
                        .map { path -> path.toString() }.toList()

                    if (selectedFilePathFile.isDirectory) {
                        // If directory, can only move outwards (above parent directory)
                        var updatedDirList = ArrayList<String>()
                        for (dir in dirList) {
                            if (!dir.startsWith(selectedFilePath)) {
                                updatedDirList.add(dir)
                            }
                        }
                        dirList = updatedDirList
                    }

                    val moveFileDialog = ChoiceDialog(selectedFilePathFile.parent, dirList)
                    moveFileDialog.title = "Move File/Directory"
                    moveFileDialog.headerText = "Please select a directory that you want to move this file to"
                    val pathToMoveFileInto = moveFileDialog.showAndWait()

                    // If this file name already exist in the path you want to move it into, throw error
                    if (filePathMapToFileName.containsKey("${pathToMoveFileInto.get()}/${selectedFilePathFile.name}")
                        && "${pathToMoveFileInto.get()}/${selectedFilePathFile.name}" != selectedFilePath) {
                        val errorPopup = Alert(Alert.AlertType.ERROR)
                        errorPopup.title = "Unable to Move File/Directory"
                        errorPopup.headerText = "Error Encountered Moving File/Directory"
                        errorPopup.contentText = "The directory entered already has an existing file/directory name."
                        errorPopup.showAndWait()
                        return
                    }

                    if (pathToMoveFileInto.isPresent) {
                        val newPath = "${pathToMoveFileInto.get()}/${selectedFilePathFile.name}"

                        if (selectedFilePathFile.parent != pathToMoveFileInto.get()) {

                            // update fileNameMapToFullPath and fileList
                            if (selectedFilePathFile.isDirectory) {
                                // need to change children of directory
                                File(newPath).mkdir()
                                copyDir(selectedFilePathFile.toPath(), File(newPath).toPath())
                                File(selectedFilePath).deleteRecursively()

                                filePathMapToFileName = getUpdatedFileNameMapToFullPathForDir(
                                    selectedFilePath,
                                    File(newPath),
                                    filePathMapToFileName
                                )
                            } else {
                                selectedFilePathFile.renameTo(File(newPath))

                                filePathMapToFileName.remove(selectedFilePathFile.path)
                                filePathMapToFileName[newPath] = File(newPath).name
                            }

                            fileList = ListView(getFileList(File(pathToMoveFileInto.get()).listFiles().toList(), showHiddenFiles))
                            fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                            fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
                            fileList.selectionModel.select(fileList.items.indexOf(getCorrectFileOrDirName(File(newPath))))
                            border.left = fileList
                            selectedFilePath = newPath
                            border.bottom = setBottomPath(selectedFilePath)
                        }
                    } else {
                        return
                    }
                } catch (e: Exception) {
                    val errorPopup = Alert(Alert.AlertType.ERROR)
                    errorPopup.title = "Unable to Move File/Directory"
                    errorPopup.headerText = "Error Encountered Moving File/Directory"
                    errorPopup.showAndWait()
                }
            }
        }

        fun moveClickHandler(): EventHandler<MouseEvent> = EventHandler {
            moveClickHandlerHelper()
        }

        // event handler for deleting file
        fun deleteClickHandlerHelper() {
            if (selectedFilePath != testDir.path && !isSelectedFileTheFolder(selectedFilePath, fileList)) {
                val confirmDeletePrompt = Alert(Alert.AlertType.CONFIRMATION)
                confirmDeletePrompt.title = "Confirm Delete"
                confirmDeletePrompt.headerText = "Are you sure you want to delete this file?"
                confirmDeletePrompt.contentText = "If you wish to delete this file, click OK. Otherwise, click Cancel."
                val result = confirmDeletePrompt.showAndWait()

                val currentFileList = fileList.items.toList().toMutableList()
                val selectedFilePathFile = File(selectedFilePath)

                if (result.isPresent && result.get() == ButtonType.OK) {
                    if (selectedFilePathFile.isDirectory) {
                        selectedFilePathFile.deleteRecursively()
                        // Update filNameMapToFullPath and fileList
                        filePathMapToFileName = filePathMapToFileName.filter { entry ->
                            !entry.key.startsWith(selectedFilePath)
                        }.toMap() as HashMap<String, String>

                        currentFileList.remove("${selectedFilePathFile.name}/")
                        border.left = fileList
                    } else {
                        File(selectedFilePath).delete()
                        // Update filNameMapToFullPath and fileList
                          filePathMapToFileName.remove(selectedFilePath)

                        currentFileList.remove("${selectedFilePathFile.name}")
                    }

                    fileList = ListView(FXCollections.observableList(currentFileList))
                    fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                    fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
                    border.left = fileList
                    border.center = null
                    selectedFilePath = selectedFilePath.substringBeforeLast("/")
                    border.bottom = setBottomPath(selectedFilePath)
                }
            }
        }

        fun deleteClickHandler(): EventHandler<MouseEvent> = EventHandler {
            deleteClickHandlerHelper()
        }

        fun prevClickHandler() {
            if (selectedFilePath != testDir.path &&
                    (File(selectedFilePath).parent != testDir.path && (!File(selectedFilePath).isFile || (!File(selectedFilePath).isDirectory && !isSelectedFileTheFolder(selectedFilePath, fileList)))
                            || (File(selectedFilePath).parent == testDir.path && File(selectedFilePath).isDirectory && isSelectedFileTheFolder(selectedFilePath, fileList)))) {
                var nextSelectedFileName: String
                if (isSelectedFileTheFolder(selectedFilePath, fileList)) {
                    fileList = ListView(getFileList(File(selectedFilePath).parentFile.listFiles().toList(), showHiddenFiles))
                    nextSelectedFileName = File(selectedFilePath).parent
                } else {
                    fileList = ListView(getFileList(File(selectedFilePath).parentFile.parentFile.listFiles().toList(), showHiddenFiles))
                    nextSelectedFileName = File(selectedFilePath).parentFile.parent
                }

                // Update the file list view to the directory selected
                fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())

                selectedFilePath = nextSelectedFileName

                border.bottom = setBottomPath(selectedFilePath)
                border.center = null
                border.left = fileList
            }
        }

        fun nextClickHandler() {
            if (File(selectedFilePath).isDirectory && selectedFilePath != testDir.path) {
                // Update the file list view to the directory selected
                fileList = ListView(getFileList(File(selectedFilePath).listFiles().toList(), showHiddenFiles))
                fileList.setOnKeyPressed {keyPressFileListHandler(it)}
                fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())

                border.center = null
                border.left = fileList
            }
        }

        fun homeClickHandler() {
            fileList = ListView(getFileList(testDir.listFiles().toList(), showHiddenFiles))
            border.left = fileList
            fileList.setOnKeyPressed {keyPressFileListHandler(it)}
            fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
            selectedFilePath = testDir.path
            border.bottom = setBottomPath(selectedFilePath)
            border.center = null
        }

        // set up menu bar except options
        fun setUpMenuBarExceptOptions(): MenuBar {
            val menuBar = MenuBar()
            // set file menu
            val fileMenu = Menu("File")
            // QUIT
            val fileQuit = MenuItem("Quit")
            fileQuit.setOnAction { Platform.exit() }
            fileMenu.items.addAll(fileQuit)

            // set view menu
            val viewMenu = Menu("View")
            val viewHome = MenuItem("Home")
            viewHome.setOnAction { homeClickHandler() }
            viewMenu.items.addAll(viewHome)

            // set actions menu
            val actionsMenu = Menu("Actions")
            // RENAME ACTION
            val actionRename = MenuItem("Rename")
            actionRename.setOnAction { renameClickHandlerHelper() }
            // MOVE ACTION
            val actionMove = MenuItem("Move")
            actionMove.setOnAction { moveClickHandlerHelper() }
            // DELETE ACTION
            val actionDelete = MenuItem("Delete")
            actionDelete.setOnAction { deleteClickHandlerHelper() }
            // NEXT
            val actionNext = MenuItem("Next")
            actionNext.setOnAction { nextClickHandler() }
            // PREV
            val actionPrev = MenuItem("Prev")
            actionPrev.setOnAction { prevClickHandler() }
            actionsMenu.items.addAll(actionRename, actionMove, actionDelete, actionNext, actionPrev)

            menuBar.menus.addAll(fileMenu, viewMenu, actionsMenu)
            return menuBar
        }

        // set up tool bar
        fun setUpToolBar(): ToolBar {
            val toolbar = ToolBar()
            // Add event handler for home
            val home = Button("Home")
            val homeClickHandler: EventHandler<MouseEvent> = EventHandler { homeClickHandler() }
            home.addEventFilter(MouseEvent.MOUSE_CLICKED, homeClickHandler)

            val prev = Button("Prev")
            val prevClickHandler: EventHandler<MouseEvent> = EventHandler { prevClickHandler() }
            prev.addEventFilter(MouseEvent.MOUSE_CLICKED, prevClickHandler)

            val next = Button("Next")
            val nextClickHandler: EventHandler<MouseEvent> = EventHandler { nextClickHandler() }
            next.addEventFilter(MouseEvent.MOUSE_CLICKED, nextClickHandler)

            // Add event handler for rename
            val delete = Button("Delete")
            delete.addEventFilter(MouseEvent.MOUSE_CLICKED, deleteClickHandler())

            // Add event handler for rename
            val rename = Button("Rename")
            rename.addEventFilter(MouseEvent.MOUSE_CLICKED, renameClickHandler())

            // Add event handler for move
            val move = Button("Move")
            move.addEventFilter(MouseEvent.MOUSE_CLICKED, moveClickHandler())

            toolbar.items.addAll(home, prev, next, delete, rename, move)
            return toolbar
        }

        // event handler when show hidden files is clicked
        fun showHiddenFilesClickHandlerHelper() {
            showHiddenFiles = !showHiddenFiles
            val selectedFilePathFile = File(selectedFilePath)

            if (!isHiddenFileOrDir(selectedFilePathFile.name) && selectedFilePathFile.isFile) {
                fileList = ListView(getFileList(selectedFilePathFile.parentFile.listFiles().toList(), showHiddenFiles))
            } else if (!isHiddenFileOrDir(selectedFilePathFile.name) && selectedFilePathFile.isDirectory) {
                fileList = if (!isSelectedFileTheFolder(selectedFilePath, fileList)) {
                    // If selected dir is within the list view -> check if selected dir is in fileList
                    ListView(getFileList(selectedFilePathFile.parentFile.listFiles().toList(), showHiddenFiles))
                } else {
                    // if selected dir is the actual dir you are in -> check selected dir is not in fileList
                    ListView(getFileList(selectedFilePathFile.listFiles().toList(), showHiddenFiles))
                }
            }

            // if hidden file/directory is selected and choose to not show hidden files, changed
            // selected file and directory to the closest non-hidden parent
            if (isHiddenFileOrDir(selectedFilePathFile.name) && !showHiddenFiles) {
                // makes sure to reassign selected file path to closest non-hidden parent
                selectedFilePath = selectedFilePath.substringBefore("/.")
                fileList = ListView(getFileList(File(selectedFilePath).listFiles().toList(), showHiddenFiles))
                border.bottom = setBottomPath(selectedFilePath)
                border.center = null
            }
            fileList.setOnKeyPressed {keyPressFileListHandler(it)}
            fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())
            border.left = fileList

            val menuBar = setUpMenuBarExceptOptions()
            // set options menu
            val optionsMenu = Menu("Options")
            // SHOW HIDDEN FILES OPTIONS
            val optionShowHiddenFiles = MenuItem(if (showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files")
            optionShowHiddenFiles.setOnAction { showHiddenFilesClickHandlerHelper() }
            optionsMenu.items.add(optionShowHiddenFiles)
            menuBar.menus.add(optionsMenu)

            val toolBar = setUpToolBar()
            val menuAndToolBar = VBox(menuBar, toolBar)
            border.top = menuAndToolBar
        }

        // add options to menu bar
        fun addOptionToMenuBar(menuBar: MenuBar): MenuBar {
            // set options menu
            val optionsMenu = Menu("Options")
            // SHOW HIDDEN FILES OPTIONS
            val optionShowHiddenFiles = MenuItem(if (showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files")
            optionShowHiddenFiles.setOnAction { showHiddenFilesClickHandlerHelper() }
            optionsMenu.items.add(optionShowHiddenFiles)

            menuBar.menus.add(optionsMenu)

            return menuBar
        }

        //////////////////////////////////////// TOP MENUBAR AND TOOLBAR ////////////////////////////////////////
        var menuBar = setUpMenuBarExceptOptions()
        menuBar = addOptionToMenuBar(menuBar)

        val toolBar = setUpToolBar()

        // stack menu and toolbar in the top region
        val menuAndToolBar = VBox(menuBar, toolBar)

        /////////////////////////////////////////// LEFT FILE LIST ///////////////////////////////////////////
        fileList.setOnKeyPressed {keyPressFileListHandler(it)}
        fileList.addEventHandler(MouseEvent.MOUSE_CLICKED, fileClickHandler())

        // SETUP LAYOUT
        border.top = menuAndToolBar
        border.left = fileList
        border.bottom = setBottomPath(selectedFilePath)

        // CREATE AND SHOW SCENE
        val scene = Scene(border, 800.0, 600.0)
        scene.setOnKeyPressed { backspaceFileListHandler(it) }
        stage.scene = scene
        stage.title = "File Browser"
        stage.isResizable = false
        stage.show()
    }
}

