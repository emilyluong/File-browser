import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import javafx.util.Duration
import java.io.File
import kotlin.properties.Delegates
import kotlin.random.Random

class SpaceInvaders: Application()  {

    enum class SCENES {
        HOMESCENE,
        LEVEL1,
        LEVEL2,
        LEVEL3
    }

    val screenWidth = 1300.0
    val screenHeight = 800.0

    var isGameOver = false
    var isGameSuccessfullyCompleted = false

    var lives = 3
    var level = 1
    var score = 0

    val PLAYER_SPEED = 3.0
    val PLAYER_BULLET_SPEED = 6.0
    var ENEMY_SPEED = 0.5
    val ENEMY_VERTICAL_SPEED = 40.0     // enemies descend one row (alien height)
    var ENEMY_BULLET_SPEED = 4.0
    var SPEED_INCREASE = 0.25

    var player = Player()
    var playerBullets = ArrayList<PlayerBullet>()
    var enemies = ArrayList<ArrayList<Enemy>>()
    var enemyBullets = ArrayList<EnemyBullet>()
    var needToSpawnPlayer = false
    var totalEnemies = 50

    val playerFireCoolDownInterval = 500      // every 30sec, player can shoot -> ensures shooting max twice per sec
    var lastTimePlayerFired = System.currentTimeMillis()

    val space = 10.0
    val enemyWidthAndSpace = 60.0 + space // 60 is the width of the enemy
    val enemyHeightAndSpace = 40.0 + space // 40 is the height of the enemy

    val enemyColNum = 10
    var isEnemiesMovingLeft = false

    var enemyFireCoolDownInterval = 8000
    var lastTimeEnemyFired = System.currentTimeMillis()

    val canvas = Canvas(screenWidth, screenHeight)
    val gc = canvas.graphicsContext2D
    var timer: Timeline? = null

    var currentScene: String by Delegates.observable(SCENES.HOMESCENE.toString()) { property, oldValue, newValue ->
        if (newValue == SCENES.HOMESCENE.toString()) {
            timer!!.stop()
        } else {
            timer!!.play()
        }
    }

    override fun start(stage: Stage) {
        // window name
        stage.title = this.javaClass.name

        timer = Timeline(KeyFrame(Duration.millis(1000.0/60), { runGame(gc, stage) }))
        timer!!.cycleCount = Animation.INDEFINITE

        setScene(stage, SCENES.HOMESCENE)
        stage.isResizable = false
        stage.show()
    }

    fun setScene(stage: Stage, scene: SCENES?) {
        when (scene) {
            SCENES.HOMESCENE -> stage.scene = setHomeScene(stage)
            SCENES.LEVEL1 -> stage.scene = setLevel1Scene(stage)
            SCENES.LEVEL2 -> stage.scene = setLevel2Scene(stage)
            SCENES.LEVEL3 -> stage.scene = setLevel3Scene(stage)
            else -> return
        }
    }

    fun setHomeScene(stage: Stage): Scene {
        currentScene = SCENES.HOMESCENE.toString()

        val homeSceneGrid = GridPane()
        homeSceneGrid.alignment = Pos.CENTER
        val homeVBox = setHomeUI()
        homeSceneGrid.children.add(homeVBox)
        val homeScene = Scene(homeSceneGrid, screenWidth, screenHeight)
        homeScene.onKeyPressed = EventHandler { event: KeyEvent ->
            when (event.code) {
                KeyCode.ENTER -> {
                    resetBackToDefault()
                    setScene(stage, SCENES.LEVEL1)
                }
                KeyCode.DIGIT1, KeyCode.NUMPAD1 -> {
                    resetBackToDefault()
                    setScene(stage, SCENES.LEVEL1)
                }
                KeyCode.DIGIT2, KeyCode.NUMPAD2 -> {
                    resetBackToDefault()
                    level = 2
                    setScene(stage, SCENES.LEVEL2)
                }
                KeyCode.DIGIT3, KeyCode.NUMPAD3 -> {
                    resetBackToDefault()
                    level = 3
                    setScene(stage, SCENES.LEVEL3)
                }
                KeyCode.Q -> Platform.exit()
            }
        }
        return homeScene
    }

    fun setHomeUI(): VBox {
        val vBox = VBox(30.0)
        vBox.alignment = Pos.CENTER

        val spaceInvadersLogo = ImageView(Image("/images/logo.png"))
        val headerText = Text("Instructions")
        headerText.font = Font.font(32.0)
        val contentText = Text(
            "ENTER - Start Game\n" +
                "A or \u25c0, D or \u25b6 - Move ship left or right\n" +
                "SPACE - Fire!\n" +
                "Q - Quit Game\n" +
                "1 or 2 or 3 - Start Game at a specific level")
        contentText.font = Font.font(18.0)
        contentText.textAlignment = TextAlignment.CENTER

        val footerText = Text("Implemented by Emily Luong for CS349, University of Waterloo, S22")

        vBox.children.addAll(spaceInvadersLogo, headerText, contentText, footerText)
        return vBox
    }

    fun setLevel1Scene(stage: Stage): Scene {
        currentScene = SCENES.LEVEL1.toString()

        ENEMY_SPEED = 0.15
        ENEMY_BULLET_SPEED = 1.0
        SPEED_INCREASE = 0.04
        enemyFireCoolDownInterval = 6000

        return setGame(stage)
    }

    fun setLevel2Scene(stage: Stage): Scene {
        currentScene = SCENES.LEVEL2.toString()

        ENEMY_SPEED = 0.25
        ENEMY_BULLET_SPEED = 3.0
        SPEED_INCREASE = 0.08
        enemyFireCoolDownInterval = 4000

        return setGame(stage)
    }

    fun setLevel3Scene(stage: Stage): Scene {
        currentScene = SCENES.LEVEL3.toString()

        ENEMY_SPEED = 0.35
        ENEMY_BULLET_SPEED = 5.0
        SPEED_INCREASE = 0.12
        enemyFireCoolDownInterval = 2000

        return setGame(stage)
    }

    fun setGame(stage: Stage): Scene {
        clearCanvas()
        setGameLabels()

        // set up player and enemies
        player.playerSpeed = PLAYER_SPEED
        player.draw(gc)
        spawnEnemies()

        val pane = Pane(canvas)

        val gameScene = Scene(pane, screenWidth, screenHeight)
        gameScene.fill = Color.BLACK
        gameScene.onKeyPressed = EventHandler { event: KeyEvent ->
            when (event.code) {
                KeyCode.RIGHT, KeyCode.D -> player.isMoveRight = true
                KeyCode.LEFT, KeyCode.A -> player.isMoveLeft = true
                KeyCode.SPACE -> {
                    if (System.currentTimeMillis() - lastTimePlayerFired > playerFireCoolDownInterval) {
                        playerBullets.add(player.shoot(PLAYER_BULLET_SPEED))
                        lastTimePlayerFired = System.currentTimeMillis()
                        MediaPlayer(Media(File("src/main/resources/sounds/shoot.wav").toURI().toString())).play()
                    }
                }
                KeyCode.ENTER -> {
                    if (isGameOver || isGameSuccessfullyCompleted) {
                        clearCanvas()
                        resetBackToDefault()
                        setScene(stage, SCENES.LEVEL1)
                    }
                }
                KeyCode.I -> {
                    if (isGameOver || isGameSuccessfullyCompleted) {
                        clearCanvas()
                        setScene(stage, SCENES.HOMESCENE)
                    }
                }
                KeyCode.Q -> {
                    if (isGameOver || isGameSuccessfullyCompleted) {
                        Platform.exit()
                    }
                }
                KeyCode.DIGIT1, KeyCode.NUMPAD1 -> {
                    if (isGameOver || isGameSuccessfullyCompleted) {
                        clearCanvas()
                        resetBackToDefault()
                        setScene(stage, SCENES.LEVEL1)
                    }
                }
                KeyCode.DIGIT2, KeyCode.NUMPAD2 -> {
                    if (isGameOver || isGameSuccessfullyCompleted) {
                        clearCanvas()
                        resetBackToDefault()
                        level = 2
                        setScene(stage, SCENES.LEVEL2)
                    }
                }
                KeyCode.DIGIT3, KeyCode.NUMPAD3 -> {
                    if (isGameOver || isGameSuccessfullyCompleted) {
                        clearCanvas()
                        resetBackToDefault()
                        level = 3
                        setScene(stage, SCENES.LEVEL3)
                    }
                }
            }
        }

        gameScene.onKeyReleased = EventHandler { event: KeyEvent ->
            when (event.code) {
                KeyCode.RIGHT, KeyCode.D -> player.isMoveRight = false
                KeyCode.LEFT, KeyCode.A -> player.isMoveLeft = false
            }
        }

        return gameScene
    }

    fun resetBackToDefault() {
        score = 0
        lives = 3
        level = 1

        isGameOver = false
        isGameSuccessfullyCompleted = false

        player = Player()
        playerBullets = ArrayList()
        enemies = ArrayList()
        enemyBullets = ArrayList()
        needToSpawnPlayer = false
        totalEnemies = 50

        lastTimePlayerFired = System.currentTimeMillis()
        isEnemiesMovingLeft = false
        lastTimeEnemyFired = System.currentTimeMillis()
    }

    fun setGameLevel(level: Int, stage: Stage) {
        when (level) {
            1 -> setScene(stage, SCENES.LEVEL1)
            2 -> setScene(stage, SCENES.LEVEL2)
            3 -> setScene(stage, SCENES.LEVEL3)
        }
    }

    fun runGame(gc: GraphicsContext, stage: Stage) {

        if (isGameOver) {
            showGameDonePopup("GAME OVER!")
            return
        } else if (isGameSuccessfullyCompleted) {
            showGameDonePopup("GAME COMPLETED!")
            return
        }

        // all enemies are killed
        if (totalEnemies == 0 && level < 3) {
            // set next level
            level++
            playerBullets.clear()
            enemyBullets.clear()
            setGameLevel(level, stage)
            totalEnemies = 50
            return
        } else if (totalEnemies == 0 && level == 3) {
            // show pop up that you finished the game
            isGameSuccessfullyCompleted = true
            return
        }

        // if need to respawn the player with remaining ships
        if (needToSpawnPlayer) {
            player.positionX = Random.nextDouble(space,screenWidth-space-player.playerImageWidth)
            player.draw(gc)
            needToSpawnPlayer = false
        }

        // handles player moving left and right
        if (player.isMoveRight) {
            player.moveRight(gc)
        } else if (player.isMoveLeft) {
            player.moveLeft(gc)
        }

        // update bullet list to keep going upwards
        val updatedBullets = ArrayList<PlayerBullet>()
        for (bullet in playerBullets) {
            if (bullet.positionY < 0) {
                continue
            } else {
                bullet.updatePositionY()
                updatedBullets.add(bullet)
            }
        }
        playerBullets = updatedBullets

        // update enemy bullet list to keep going downwards
        val updatedEnemyBullet = ArrayList<EnemyBullet>()
        for (enemyBullet in enemyBullets) {
            if (enemyBullet.positionY > screenHeight) {
                continue
            } else {
                enemyBullet.updatePositionY()
                updatedEnemyBullet.add(enemyBullet)
            }
        }
        enemyBullets = updatedEnemyBullet

        // clears the canvas and updates the bullets and enemies moving on the screen
        clearCanvas()
        player.draw(gc)

        var moveEnemyDown = false
        if (!isEnemiesMovingLeft && enemies[0][enemyColNum - 1].positionX > screenWidth - enemyWidthAndSpace) {
            //moving right and enemies reach right side of screen then move left
            isEnemiesMovingLeft = true
            moveEnemyDown = true
        } else if (isEnemiesMovingLeft && enemies[0][0].positionX < space) {
            //moving left and enemies reach the left side of the screen move right
            isEnemiesMovingLeft = false
            moveEnemyDown = true
        }

        if (moveEnemyDown) {
            // end of screen reached, pick a random enemy to shoot down at the player
            randomEnemyShoot()
        } else {
            // when enemies moving, shoot from random enemy
            if (System.currentTimeMillis() - lastTimeEnemyFired > enemyFireCoolDownInterval) {
                randomEnemyShoot()
                lastTimeEnemyFired = System.currentTimeMillis()
            }
        }

        // handle situation when enemy bullet hits the player
        val updatedEnemyBullets = ArrayList<EnemyBullet>()
        for (enemyBullet in enemyBullets) {
            if (enemyBullet.isCollided(player)) {
                // if collided with player, delete player and respawn it on random posY and update lives
                MediaPlayer(Media(File("src/main/resources/sounds/explosion.wav").toURI().toString())).play()
                handlePlayerCollision()
                val updatedScore = score - getEnemyBulletScore(enemyBullet)
                if (updatedScore < 0) {
                    score = 0
                } else {
                    score = updatedScore
                }
            } else {
                updatedEnemyBullets.add(enemyBullet)
            }
        }
        enemyBullets = updatedEnemyBullets

        // handles situations when all enemies (including killed) hits the bottom of the screen
        if (isEnemyHitBottom(enemies[0][0])) {
            MediaPlayer(Media(File("src/main/resources/sounds/explosion.wav").toURI().toString())).play()
            isGameOver = true
        }

        for (row in enemies.indices) {
            for (enemy in enemies[row]) {
                // moving enemy to the left or right
                if (isEnemiesMovingLeft) {
                    enemy.moveEnemyToLeft()
                } else {
                    enemy.moveEnemyToRight()
                }

                // moving enemy down when reached end of the screen
                if (moveEnemyDown) {
                    enemy.moveEnemyDown(gc)
                }

                // handle situation when enemy hits the player
                if (player.isCollided(enemy) && !enemy.isKilled) {
                    MediaPlayer(Media(File("src/main/resources/sounds/explosion.wav").toURI().toString())).play()
                    handlePlayerCollision()
                }

                // update the bullets to handle collision with an enemy
                val updatedBullets = ArrayList<PlayerBullet>()
                for (bullet in playerBullets) {
                    if (bullet.isCollided(enemy) && !enemy.isKilled) {
                        MediaPlayer(Media(File("src/main/resources/sounds/invaderkilled.wav").toURI().toString())).play()
                        enemy.isKilled = true
                        totalEnemies--
                        // increase the score
                        score += getEnemyScore(enemy)
                        increaseEnemySpeed()
                    } else {
                        updatedBullets.add(bullet)
                    }
                }
                playerBullets = updatedBullets

                // only redraw enemies that are not killed
                if (!enemy.isKilled) {
                    enemy.draw(gc)
                }
            }
        }

        // re drawing components in the game
        setGameLabels()
        for (bullet in playerBullets) {
            bullet.draw(gc)
        }
        for (enemyBullet in enemyBullets) {
            enemyBullet.draw(gc)
        }
    }

    fun handlePlayerCollision() {
        lives--
        if (lives < 1) {
            isGameOver = true
        }
        removePlayerFromScreen()
        needToSpawnPlayer = true
    }

    fun isEnemyHitBottom(enemy: Enemy): Boolean {
        // check the last row (not necessarily the last row bc player could've killed last rows
        val enemyBottomPosY = enemy.positionY + enemy.enemyImageHeight
        return enemyBottomPosY > screenHeight
    }

    fun increaseEnemySpeed() {
        ENEMY_SPEED += SPEED_INCREASE

        for (row in enemies.indices) {
            for (col in enemies[row].indices) {
                enemies[row][col].enemySpeed = ENEMY_SPEED
            }
        }
    }

    fun randomEnemyShoot() {
        var randRow: Int
        var randCol: Int
        while(true) {
            randRow = (0..4).random()
            randCol = (0..9).random()

            if (!enemies[randRow][randCol].isKilled) {
                break
            }
        }
        val enemyShooting = enemies[randRow][randCol]

        val enemyBulletImg: Image
        if (randRow == 0) {
            enemyBulletImg = Image("/images/bullet3.png")
            MediaPlayer(Media(File("src/main/resources/sounds/fastinvader3.wav").toURI().toString())).play()

        } else if (randRow == 1 || randRow == 2) {
            enemyBulletImg = Image("/images/bullet2.png")
            MediaPlayer(Media(File("src/main/resources/sounds/fastinvader2.wav").toURI().toString())).play()

        } else {
            enemyBulletImg = Image("/images/bullet1.png")
            MediaPlayer(Media(File("src/main/resources/sounds/fastinvader1.wav").toURI().toString())).play()
        }

        val enemyBulletPosX = enemyShooting.positionX + enemyShooting.enemyImageWidth/2 - 7.5     // 7.5 in half of bullet width
        val enemyBulletPosY = enemyShooting.positionY + enemyShooting.enemyImageHeight
        val enemyBullet = EnemyBullet(enemyBulletImg, enemyBulletPosX, enemyBulletPosY, ENEMY_BULLET_SPEED)
        enemyBullets.add(enemyBullet)

    }

    fun showGameDonePopup(title: String) {
        gc.fill = Color.WHITE
        gc.fillRoundRect(screenWidth/4, screenHeight/4, screenWidth/4 * 2, screenHeight/4 * 2, 25.0, 25.0)
        gc.fill = Color.BLACK

        gc.textAlign = TextAlignment.CENTER;
        gc.font = Font.font("Arial", FontWeight.EXTRA_BOLD, 40.0)
        gc.fillText(
            title,
            screenWidth/2,
            screenHeight/4 + 100
        )

        gc.font = Font.font("Arial", FontWeight.NORMAL, 24.0)
        gc.fillText(
            "Final Score: $score",
            screenWidth/2,
            screenHeight/4 + 100 + 45
        )

        gc.fillText(
            "ENTER - Start New Game\n" +
                    "I - Back to Instructions\n" +
                    "Q - Quit Game\n" +
                    "1 or 2 or 3 - Start New Game at a Specific Level\n",
            screenWidth/2,
            screenHeight/4 * 2
        )
    }

    fun getEnemyScore(enemy: Enemy): Int {
        val enemyImg = enemy.enemyImage
        if (enemyImg !== null) {
            return if (enemyImg.url == "/images/enemy1.png") {
                100
            } else if (enemyImg.url == "/images/enemy2.png") {
                200
            } else {
                300
            }
        }
        return 0
    }

    fun getEnemyBulletScore(enemyBullet: EnemyBullet): Int {
        val enemyBulletImg = enemyBullet.enemyBulletImage
        if (enemyBulletImg !== null) {
            return if (enemyBulletImg.url == "/images/bullet1.png") {
                100
            } else if (enemyBulletImg.url == "/images/bullet2.png") {
                200
            } else {
                300
            }
        }
        return 0
    }

    fun spawnEnemies() {

        var currPosX = screenWidth/4 + space
        var currPosY = 80.0 // to account for the labels at the top
        val updatedEnemies = ArrayList<ArrayList<Enemy>>()

        for (i in 0 until 5) {
            val row = ArrayList<Enemy>()
            for (j in 0 until 10) {
                if (i == 0) {
                    val enemy = Enemy(Image("/images/enemy3.png"), currPosX, currPosY, ENEMY_SPEED, ENEMY_VERTICAL_SPEED)
                    enemy.draw(gc)
                    row.add(enemy)
                    currPosX += enemyWidthAndSpace
                } else if (i == 1 || i == 2) {
                    val enemy = Enemy(Image("/images/enemy2.png"), currPosX, currPosY, ENEMY_SPEED, ENEMY_VERTICAL_SPEED)
                    enemy.draw(gc)
                    row.add(enemy)
                    currPosX += enemyWidthAndSpace
                } else {
                    val enemy = Enemy(Image("/images/enemy1.png"), currPosX, currPosY, ENEMY_SPEED, ENEMY_VERTICAL_SPEED)
                    enemy.draw(gc)
                    row.add(enemy)
                    currPosX += enemyWidthAndSpace
                }
            }
            updatedEnemies.add(row)
            currPosY += enemyHeightAndSpace
            currPosX = screenWidth/4 + space
        }

        enemies = updatedEnemies
    }

    fun removePlayerFromScreen() {
        gc.clearRect(player.positionX, player.positionY, player.playerImageWidth, player.playerImageHeight)
    }

    fun clearCanvas() {
        gc.clearRect(0.0, 0.0, screenWidth, screenHeight)
    }

    fun setGameLabels() {
        gc.textAlign = TextAlignment.LEFT;
        gc.fill = Color.WHITE
        gc.font = Font.font("Arial", FontWeight.EXTRA_BOLD, 18.0)
        gc.fillText("SCORE: $score", 40.0, 30.0)
        gc.fillText("LIVES: $lives", screenWidth - 250.0, 30.0)
        gc.fillText("LEVEL: $level", screenWidth - 120.0, 30.0)
    }
}