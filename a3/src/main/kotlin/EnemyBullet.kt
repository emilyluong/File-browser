import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image

class EnemyBullet {

    var enemyBulletImage: Image? = null
    val enemyBulletImageWidth = 15.0
    val enemyBulletImageHeight = 35.0

    var positionX = 0.0
    var positionY = 0.0

    var bulletSpeed = 0.0

    constructor(enemyBullet: Image, posX: Double, posY: Double, speed: Double) {
        enemyBulletImage = enemyBullet
        positionX = posX
        positionY = posY
        bulletSpeed = speed
    }

    fun draw(gc: GraphicsContext) {
        gc.drawImage(enemyBulletImage, positionX, positionY, enemyBulletImageWidth, enemyBulletImageHeight)
    }

    fun updatePositionY() {
        positionY += bulletSpeed
    }

    fun isCollided(player: Player): Boolean {
        val playerTopPosY = player.positionY
        val playerLeftPosX = player.positionX
        val playerRightPosX = player.positionX + player.playerImageWidth

        val bulletBottomPosY = positionY + enemyBulletImageHeight
        val bulletLeftPosX = positionX
        val bulletRightPosX = positionX + enemyBulletImageWidth

        return bulletBottomPosY > playerTopPosY && bulletLeftPosX < playerRightPosX && bulletRightPosX > playerLeftPosX
    }

}