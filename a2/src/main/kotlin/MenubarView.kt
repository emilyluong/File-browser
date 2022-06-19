import javafx.scene.control.*
import javafx.scene.input.DataFormat
import javafx.scene.layout.VBox

internal class MenubarView (private val model: Model) : VBox(), IView {

    private val menubar = MenuBar()

    fun setEditMenu(isDisabled: Boolean): Menu {
        val edit = Menu("Edit")

        val cutEdit = MenuItem("Cut")
        cutEdit.setOnAction {
            model.cutShape()
        }
        cutEdit.isDisable = isDisabled

        val copyEdit = MenuItem("Copy")
        copyEdit.setOnAction {
            model.copyShape()
        }
        copyEdit.isDisable = isDisabled

        val pasteEdit = MenuItem("Paste")
        pasteEdit.setOnAction {
            model.pasteShape()
        }
        if (!model.clipboard.hasContent(DataFormat.PLAIN_TEXT)) {
            pasteEdit.isDisable = isDisabled
        }

        edit.items.addAll(cutEdit, copyEdit, pasteEdit)
        return edit
    }

    fun setFileMenu(): Menu {
        val file = Menu("File")
        val newFile = MenuItem("New")
        newFile.setOnAction {
            model.newFile()
        }
        val loadFile = MenuItem("Load")
        loadFile.setOnAction {
            model.loadFile()
        }
        val saveFile = MenuItem("Save")
        saveFile.setOnAction {
            model.saveFile()
        }
        val quitFile = MenuItem("Quit")
        quitFile.setOnAction {
            model.exit()
        }

        file.items.addAll(newFile, loadFile, saveFile, quitFile)
        return file
    }

    fun setHelpMenu(): Menu {
        val help = Menu("Help")
        val aboutHelp = MenuItem("About")
        aboutHelp.setOnAction {
            val infoPopup = Alert(Alert.AlertType.INFORMATION)
            infoPopup.title = "Sketch It"
            infoPopup.headerText = "Sketch It Information"
            infoPopup.contentText = "Application Name: Sketch It \n Student: Emily Luong \n WatID: e4luong"
            infoPopup.showAndWait()
        }

        help.items.add(aboutHelp)
        return help
    }

    // When notified by the model that things have changed,
    // update to display the new value
    override fun updateView() {
        val file: Menu
        val edit: Menu
        val help: Menu

        if (model.currentSelectedShape == null) {
            file = setFileMenu()
            edit = setEditMenu(true)
            help = setHelpMenu()
        } else {
            file = setFileMenu()
            edit = setEditMenu(false)
            help = setHelpMenu()
        }
        menubar.menus.remove(0, menubar.menus.size)
        menubar.menus.addAll(file, edit, help)
    }

    init {

        val file = setFileMenu()
        val edit = setEditMenu(false)
        val help = setHelpMenu()

        menubar.menus.addAll(file, edit, help)

        // add menubar widget to the pane
        children.add(menubar)

        // register with the model when we're ready to start receiving data
        model.addView(this)
    }
}