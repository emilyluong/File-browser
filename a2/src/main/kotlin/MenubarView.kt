import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import jdk.jfr.consumer.EventStream.openFile


internal class MenubarView (private val model: Model) : VBox(), IView {

    private val menubar = MenuBar()

    // When notified by the model that things have changed,
    // update to display the new value
    override fun updateView() {
        println("MenubarView: updateView")
    }

    init {

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

        menubar.menus.addAll(file, help)

        // add menubar widget to the pane
        children.add(menubar)

        // register with the model when we're ready to start receiving data
        model.addView(this)
    }
}