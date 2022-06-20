import javafx.application.Application
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage


// Sample code used from CS349:
// MVC2 class repo demo
// Dragging repo demo
// Cut-Copy-Paste sample from Widgets slides

class SketchIt : Application() {

    override fun start(stage: Stage) {
        val minWidthViewPort = 800.0
        val minHeightViewPort = 400.0
        val maxWidthViewPort = 1400.0
        val maxHeightViewPort = 1000.0
        val toolPrefWidth = 250.0
        val startUpWidth = 1300.0
        val startUpHeight = 700.0

        // window name
        stage.title = "Sketch It"

        // create and initialize the Model to hold our counter
        val model = Model(stage)
        val root = BorderPane()

        // create each view, and tell them about the model
        // the views will register themselves with the model
        val menubarView = MenubarView(model)
        val canvasView = CanvasView(model)
        val toolbarView = ToolbarView(model)
        val toolbarOptionsView = ToolbarPropertyView(model)

        val content = HBox()
        val tools = VBox(20.0)
        tools.prefWidth = toolPrefWidth
        tools.background = Background(BackgroundFill(Color.GRAY, CornerRadii.EMPTY, Insets.EMPTY))
        tools.children.addAll(toolbarView, toolbarOptionsView)

        val backGroundCanvasView = StackPane(canvasView)
        canvasView.setPrefSize(maxWidthViewPort-toolPrefWidth, maxWidthViewPort)
        canvasView.setMinSize(maxWidthViewPort-toolPrefWidth, maxWidthViewPort)
        canvasView.setMaxSize(maxWidthViewPort-toolPrefWidth, maxWidthViewPort)
        backGroundCanvasView.style = "-fx-background-color: white"
        backGroundCanvasView.setPrefSize(maxWidthViewPort-toolPrefWidth, maxHeightViewPort)
        backGroundCanvasView.setMinSize(maxWidthViewPort-toolPrefWidth, maxWidthViewPort)
        backGroundCanvasView.setMaxSize(maxWidthViewPort-toolPrefWidth, maxHeightViewPort)

        val anchorPaneCanvasView = AnchorPane(backGroundCanvasView)
        anchorPaneCanvasView.setPrefSize(maxWidthViewPort-toolPrefWidth, maxHeightViewPort)
        anchorPaneCanvasView.setMinSize(maxWidthViewPort-toolPrefWidth, maxWidthViewPort)
        anchorPaneCanvasView.setMaxSize(maxWidthViewPort-toolPrefWidth, maxHeightViewPort)

        val scrollPaneCanvasView = ScrollPane(anchorPaneCanvasView)
        scrollPaneCanvasView.isFitToWidth = true
        scrollPaneCanvasView.isFitToHeight = true

        content.children.addAll(tools, scrollPaneCanvasView)

        root.top = menubarView
        root.left = tools
        root.center = content

        // Add grid to a scene (and the scene to the stage)
        val scene = Scene(root, startUpWidth, startUpHeight)

        // listeners for resizing of the stage
        fun stageSizeListener(): ChangeListener<Number> = ChangeListener { _, _, _ ->
            canvasView.resizeCanvas(stage.width, stage.height)

            // set on key press event handler for erase
            scene.setOnKeyPressed {
                println(model.selectedTool + " " + model.currentSelectedShape + " " + it.code)
                val selectedShape = model.currentSelectedShape
                if (model.selectedTool == "select" && selectedShape != null && (it.code == KeyCode.BACK_SPACE || it.code == KeyCode.DELETE)) {
                    canvasView.gc.setEffect(null)
                    model.eraseShape(selectedShape, true)
                    canvasView.gc.clearRect(0.0, 0.0, canvasView.width, canvasView.height)
                    canvasView.refreshCanvas()
                    model.notifyObservers()
                } else if (model.selectedTool == "select" && selectedShape != null && it.code == KeyCode.ESCAPE) {
                    canvasView.removeSelectedHighlightAndRefresh(selectedShape)
                    model.currentSelectedShape = null
                    model.notifyObservers()
                }

                val keyCombCopyMac = KeyCodeCombination(KeyCode.C, KeyCodeCombination.META_DOWN)
                val keyCombCopyWin = KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN)
                if ((keyCombCopyMac.match(it) || keyCombCopyWin.match(it)) && model.selectedTool == "select" && selectedShape != null) {
                    model.copyShape()
                    it.consume()
                }

                val keyCombPasteMac = KeyCodeCombination(KeyCode.V, KeyCodeCombination.META_DOWN)
                val keyCombPasteWin = KeyCodeCombination(KeyCode.V, KeyCodeCombination.CONTROL_DOWN)
                if (keyCombPasteMac.match(it) || keyCombPasteWin.match(it)) {
                    model.pasteShape()
                    it.consume()
                }

                val keyCombCutMac = KeyCodeCombination(KeyCode.X, KeyCodeCombination.META_DOWN)
                val keyCombCutWin = KeyCodeCombination(KeyCode.X, KeyCodeCombination.CONTROL_DOWN)
                if ((keyCombCutMac.match(it) || keyCombCutWin.match(it)) && model.selectedTool == "select" && selectedShape != null) {
                    model.cutShape()
                    it.consume()
                }
            }
        }
        stage.widthProperty().addListener(stageSizeListener())
        stage.heightProperty().addListener(stageSizeListener())

        stage.minWidth = minWidthViewPort
        stage.minHeight = minHeightViewPort
        stage.scene = scene
        stage.show()
    }
}