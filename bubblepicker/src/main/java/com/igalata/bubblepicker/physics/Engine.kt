package com.igalata.bubblepicker.physics

import com.igalata.bubblepicker.rendering.Item
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.contacts.Contact
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Created by irinagalata on 1/26/17.
 */
object Engine : ContactListener {

    val selectedBodies: List<CircleBody>
        get() = bodies.filter { it.increased || it.toBeIncreased || it.isIncreasing }
    var maxSelectedCount: Int? = null

    /**
     * Represents what percentage of smaller screen dimension each bubble takes.
     * Values 0..100 represent 15..35% of dimension
     */
    var radius = 50
        set(value) {
            field = value
            bubbleRadius = interpolate(0.15f, 0.35f, value / 100f)
        }
    private var bubbleRadius = 0.15f

    private val world = World(Vec2(0f, 0f), false)
    private val step = 1.0f / 60.0f
    private val bodies: ArrayList<CircleBody> = ArrayList()
    private var borders: ArrayList<Border> = ArrayList()
    private val resizeStep = 0.005f
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    private val toBeResized = ArrayList<Item>()

    override fun endContact(p0: Contact?) {
    }

    override fun beginContact(p0: Contact) {
        borders.forEach { border ->
            if (border.itemBody == p0.fixtureA.body || border.itemBody == p0.fixtureB.body) {
                val circle = if (border.itemBody == p0.fixtureA) p0.fixtureA else p0.fixtureB
                var vel = circle.body.linearVelocity
                vel = if (border.view == Border.HORIZONTAL) {
                    Vec2(vel.x, -vel.y)
                } else {
                    Vec2(-vel.x, vel.y)
                }
                circle.body.linearVelocity = vel
                return@forEach
            }
        }
    }

    override fun preSolve(p0: Contact?, p1: Manifold?) {
    }

    override fun postSolve(p0: Contact?, p1: ContactImpulse?) {
    }

    fun build(bodiesCount: Int, scaleX: Float, scaleY: Float): List<CircleBody> {
        val density = interpolate(0.8f, 0.2f, radius / 100f)
        val scaleRatio = min(scaleX, scaleY) / max(scaleX, scaleY)
        for (i in 0..bodiesCount - 1) {
            val x = Random().nextFloat() - 0.5f
            val y = Random().nextFloat() - 0.5f
            val vx = (if (Random().nextBoolean()) -0.01f else 0.01f) * Random().nextFloat() / scaleY
            val vy = (if (Random().nextBoolean()) -0.01f else 0.01f) * Random().nextFloat() / scaleY
            bodies.add(CircleBody(world, Vec2(x, y), bubbleRadius * scaleRatio, (bubbleRadius * scaleRatio) * 1.1f, density, Vec2(vx, vy)))
        }
        this.scaleX = scaleX
        this.scaleY = scaleY
        createBorders()

        world.setContactListener(this)

        bodies.forEach { body ->
            body.physicalBody.apply {
                applyLinearImpulse(body.initialForce, Vec2(0f, 0f))
            }
        }

        return bodies
    }

    fun move() {
        toBeResized.forEach { it.circleBody.resize(resizeStep) }
        world.step(step, 8, 8)
        toBeResized.removeAll(toBeResized.filter { it.circleBody.finished })
    }

    fun release() {
        world.setContactListener(null)
        touch = false
    }

    fun clear() {
        borders.forEach { world.destroyBody(it.itemBody) }
        bodies.forEach { world.destroyBody(it.physicalBody) }
        borders.clear()
        bodies.clear()
    }

    fun resize(item: Item): Boolean {
        if (selectedBodies.size >= maxSelectedCount ?: bodies.size && !item.circleBody.increased) return false

        if (item.circleBody.isBusy) return false

        item.circleBody.defineState()

        toBeResized.add(item)

        val itemCenter = item.circleBody.physicalBody.worldCenter
        bodies.filter { it.physicalBody != item.circleBody.physicalBody }.forEach {
            var v = it.physicalBody.worldCenter.sub(itemCenter).mul(0.0025f)
            v = if (item.circleBody.toBeDecreased) v.negate() else v
            it.physicalBody.applyLinearImpulse(v, itemCenter)
        }

        return true
    }

    private fun createBorders() {
        borders = arrayListOf(
                Border(world, Vec2(0f, 0.5f / scaleY), Border.HORIZONTAL),
                Border(world, Vec2(0f, -0.5f / scaleY), Border.HORIZONTAL),
                Border(world, Vec2(0.5f / scaleX, 0f), Border.VERTICAL),
                Border(world, Vec2(-0.5f / scaleX, 0f), Border.VERTICAL)
        )
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)

}