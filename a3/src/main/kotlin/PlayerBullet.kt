import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image

class PlayerBullet {

    val playerBulletImage = Image("/images/player_bullet.png")
    val playerBulletImageWidth = 6.0
    val playerBulletImageHeight = 25.0

    var positionX = 0.0
    var positionY = 0.0

    var bulletSpeed = 0.0

    constructor(posX: Double, posY: Double, speed: Double) {
        positionX = posX
        positionY = posY
        bulletSpeed = speed
    }

    fun draw(gc: GraphicsContext) {
        gc.drawImage(playerBulletImage, positionX, positionY, playerBulletImageWidth, playerBulletImageHeight)
    }

    fun updatePositionY() {
        positionY -= bulletSpeed
    }

    fun isCollided(enemy: Enemy): Boolean {
        val enemyTopPosY = enemy.positionY
        val enemyBottomPosY = enemy.positionY + enemy.enemyImageHeight
        val enemyLeftPosX = enemy.positionX
        val enemyRightPosX = enemy.positionX + enemy.enemyImageWidth

        val bulletTopPosY = positionY
        val bulletBottomPosY = positionY + playerBulletImageHeight
        val bulletLeftPosX = positionX
        val bulletRightPosX = positionX + playerBulletImageWidth


        return (!enemy.isKilled &&
            bulletTopPosY < enemyBottomPosY && bulletBottomPosY > enemyTopPosY && bulletLeftPosX < enemyRightPosX && bulletRightPosX > enemyLeftPosX)
    }

}