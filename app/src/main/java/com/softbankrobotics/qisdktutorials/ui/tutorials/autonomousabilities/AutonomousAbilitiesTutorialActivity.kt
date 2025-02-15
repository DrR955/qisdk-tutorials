/*
 * Copyright (C) 2018 Softbank Robotics Europe
 * See COPYING for the license
 */

package com.softbankrobotics.qisdktutorials.ui.tutorials.autonomousabilities

import android.os.Bundle

import com.aldebaran.qi.Consumer
import com.aldebaran.qi.sdk.Qi
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.HolderBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.holder.AutonomousAbilitiesType
import com.aldebaran.qi.sdk.`object`.holder.Holder
import com.softbankrobotics.qisdktutorials.R
import com.softbankrobotics.qisdktutorials.databinding.ActivityAutonomousAbilitiesTutorialBinding
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationBinder
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationItemType
import com.softbankrobotics.qisdktutorials.ui.tutorials.TutorialActivity
import com.softbankrobotics.qisdktutorials.utils.Constants

/**
 * The activity for the autonomous abilities tutorial.
 */
class AutonomousAbilitiesTutorialActivity : TutorialActivity(), RobotLifecycleCallbacks {

    private lateinit var binding: ActivityAutonomousAbilitiesTutorialBinding

    private var conversationBinder: ConversationBinder? = null

    // A boolean used to store the abilities status.
    private var abilitiesHeld = false

    // The holder for the abilities.
    private var holder: Holder? = null

    // The QiContext provided by the QiSDK.
    private var qiContext: QiContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAutonomousAbilitiesTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the button onClick listener.
        binding.buttonSwitchAutonomous.setOnClickListener {
            // Check that the Activity owns the focus.
            if (qiContext != null) {
                toggleAbilities()
            }
        }

        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this)
    }

    override fun onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    override val layoutId = R.layout.activity_autonomous_abilities_tutorial

    override fun onRobotFocusGained(qiContext: QiContext) {
        // Store the provided QiContext.
        this.qiContext = qiContext

        // Bind the conversational events to the view.
        val conversationStatus = qiContext.conversation.status(qiContext.robotContext)
        conversationBinder = binding.conversationView.bindConversationTo(conversationStatus)

        val say = SayBuilder.with(qiContext)
            .withText("My autonomous abilities can be disabled: click on the button to hold/release them.")
            .withLocale(Constants.Locals.ENGLISH_LOCALE)
            .build()

        say.run()
    }

    override fun onRobotFocusLost() {
        conversationBinder?.unbind()

        // Remove the QiContext.
        this.qiContext = null
    }

    override fun onRobotFocusRefused(reason: String) {
        // Nothing here.
    }

    private fun toggleAbilities() {
        // Disable the button.
        binding.buttonSwitchAutonomous.isEnabled = false

        if (abilitiesHeld) {
            holder?.let { releaseAbilities(it) }
        } else {
            holdAbilities(qiContext)
        }
    }

    private fun holdAbilities(qiContext: QiContext?) {
        // Build and store the holder for the abilities.
        val holder = HolderBuilder.with(qiContext)
            .withAutonomousAbilities(
                AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                AutonomousAbilitiesType.BASIC_AWARENESS,
                AutonomousAbilitiesType.AUTONOMOUS_BLINKING
            )
            .build()
            .also { this.holder = it }

        // Hold the abilities asynchronously.
        val holdFuture = holder.async().hold()

        // Chain the hold with a lambda on the UI thread.
        holdFuture.andThenConsume(Qi.onUiThread(Consumer<Void> {
            displayLine("Abilities held.", ConversationItemType.INFO_LOG)
            // Store the abilities status.
            abilitiesHeld = true
            // Change the button text.
            binding.buttonSwitchAutonomous.setText(R.string.release)
            // Enable the button.
            binding.buttonSwitchAutonomous.isEnabled = true
        }))
    }

    private fun releaseAbilities(holder: Holder) {
        // Release the holder asynchronously.
        val releaseFuture = holder.async().release()

        // Chain the release with a lambda on the UI thread.
        releaseFuture.andThenConsume(Qi.onUiThread(Consumer<Void> {
            displayLine("Abilities released.", ConversationItemType.INFO_LOG)
            // Store the abilities status.
            abilitiesHeld = false
            // Change the button text.
            binding.buttonSwitchAutonomous.text = getString(R.string.hold)
            // Enable the button.
            binding.buttonSwitchAutonomous.isEnabled = true
        }))
    }

    private fun displayLine(text: String, type: ConversationItemType) {
        runOnUiThread { binding.conversationView.addLine(text, type) }
    }
}
