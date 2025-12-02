package com.example.rotary_dialer

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Handles injection of digit inputs into the system dial pad
 * with comprehensive debugging and multiple search strategies
 */
class InputInjector(private val accessibilityService: AccessibilityService) {
    
    companion object {
        private const val TAG = "InputInjector"
    }
    
    /**
     * Inject a digit into the dial pad by clicking the actual button
     */
    fun injectDigit(digit: String): Boolean {
        Log.d(TAG, "\n\n=== INJECT DIGIT START: '$digit' ===")
        
        try {
            if (digit.length != 1 || !digit[0].isDigit()) {
                Log.e(TAG, "Invalid digit: $digit")
                return false
            }
            
            val detectorService = DialPadDetectorService.instance
            if (detectorService == null) {
                Log.e(TAG, "DialPadDetectorService.instance is null - accessibility service not running")
                return false
            }
            
            Log.d(TAG, "DialPadDetectorService found, attempting button click...")
            
            // Method 1: Try to find and click the actual dial pad button
            if (clickDialPadButton(digit)) {
                Log.d(TAG, "✅ SUCCESS: Digit $digit injected by clicking button")
                return true
            }
            
            Log.w(TAG, "Button click failed, trying fallback text injection...")
            
            // Method 2: Fallback to text injection
            val numberField = detectorService.getCurrentInputField()
            if (numberField != null && isNodeValid(numberField)) {
                Log.d(TAG, "Number field found, attempting direct text injection...")
                if (injectTextDirectly(numberField, digit)) {
                    Log.d(TAG, "✅ SUCCESS: Digit $digit injected via direct text")
                    return true
                }
            } else {
                Log.e(TAG, "Number field not found or invalid")
            }
            
            Log.e(TAG, "❌ FAILED: All injection methods failed for digit: $digit")
            return false
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception injecting digit $digit", e)
            return false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "❌ Invalid state when injecting digit $digit", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error injecting digit $digit", e)
            return false
        } finally {
            Log.d(TAG, "=== INJECT DIGIT END: '$digit' ===\n")
        }
    }
    
    /**
     * Find and click the actual dial pad button for the digit
     */
    private fun clickDialPadButton(digit: String): Boolean {
        Log.d(TAG, "=== Attempting to click dial pad button for digit: $digit ===")
        
        val rootNode = accessibilityService.rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "Root node is null - accessibility service may not have access")
            return false
        }
        
        Log.d(TAG, "Root node found: ${rootNode.packageName}")
        
        try {
            // First, let's explore what's available
            logNodeHierarchy(rootNode, 0, digit)
            
            // Find button with the digit text
            val button = findButtonWithText(rootNode, digit)
            if (button != null && isNodeValid(button)) {
                Log.d(TAG, "Found button for digit $digit, attempting click...")
                val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Click result for digit $digit: $clicked")
                button.recycle()
                return clicked
            } else {
                Log.e(TAG, "No button found for digit: $digit")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking dial pad button for digit $digit", e)
        } finally {
            rootNode.recycle()
        }
        
        return false
    }
    
    /**
     * Find a button node with specific text using multiple strategies
     */
    private fun findButtonWithText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        Log.d(TAG, "Searching for button with text: '$text'")
        
        // Strategy 1: Direct text/description match
        val directMatch = findByDirectMatch(node, text)
        if (directMatch != null) {
            Log.d(TAG, "Found direct match for '$text'")
            return directMatch
        }
        
        // Strategy 2: Resource ID match (common patterns)
        val resourceIdMatch = findByResourceId(node, text)
        if (resourceIdMatch != null) {
            Log.d(TAG, "Found resource ID match for '$text'")
            return resourceIdMatch
        }
        
        // Strategy 3: Button class with text content
        val buttonMatch = findButtonByClass(node, text)
        if (buttonMatch != null) {
            Log.d(TAG, "Found button class match for '$text'")
            return buttonMatch
        }
        
        Log.w(TAG, "No button found for digit: '$text'")
        return null
    }
    
    private fun findByDirectMatch(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        try {
            val nodeText = node.text?.toString()
            val nodeDesc = node.contentDescription?.toString()
            
            // Exact match
            if (nodeText == text || nodeDesc == text) {
                return if (node.isClickable) node else null
            }
            
            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findByDirectMatch(child, text)
                    if (result != null) return result
                } finally {
                    if (child != node) child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct match search", e)
        }
        return null
    }
    
    private fun findByResourceId(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        try {
            val resourceId = node.viewIdResourceName
            
            // Comprehensive resource ID patterns for different dialer apps
            // Map digit to word form for Google Dialer
            val digitWord = when(text) {
                "0" -> "zero"
                "1" -> "one"
                "2" -> "two"
                "3" -> "three"
                "4" -> "four"
                "5" -> "five"
                "6" -> "six"
                "7" -> "seven"
                "8" -> "eight"
                "9" -> "nine"
                else -> text
            }
            
            val digitPatterns = listOf(
                // Google Dialer patterns (uses word form!)
                digitWord,
                "dialpad_$digitWord",
                // Standard numeric patterns
                "digit_$text",
                "key_$text", 
                "button_$text",
                "dialpad_key_$text",
                "num_$text",
                // Samsung Dialer patterns
                "btn_$text",
                "dialkey_$text",
                "keypad_$text",
                // AOSP/Generic patterns
                "dialer_key_$text",
                "phone_key_$text",
                "pad_key_$text",
                // Alternative patterns
                "${text}_key",
                "${text}_button",
                "number_$text",
                // Just the digit itself
                text
            )
            
            if (resourceId != null) {
                Log.d(TAG, "Checking resource ID: $resourceId")
                
                for (pattern in digitPatterns) {
                    if (resourceId.contains(pattern, ignoreCase = true)) {
                        Log.d(TAG, "✅ Found resource ID match: $resourceId matches pattern '$pattern' for digit $text")
                        return if (node.isClickable) {
                            node
                        } else {
                            Log.w(TAG, "Resource ID match found but node is not clickable")
                            null
                        }
                    }
                }
                
                // Also check if resource ID ends with the digit
                if (resourceId.endsWith(text) || resourceId.endsWith("_$text")) {
                    Log.d(TAG, "✅ Found resource ID ending match: $resourceId for digit $text")
                    return if (node.isClickable) node else null
                }
            }
            
            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findByResourceId(child, text)
                    if (result != null) return result
                } finally {
                    if (child != node) child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in resource ID search", e)
        }
        return null
    }
    
    private fun findButtonByClass(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        try {
            val nodeClass = node.className?.toString()
            val nodeText = node.text?.toString()
            val nodeDesc = node.contentDescription?.toString()
            
            // Look for button-like classes with our digit
            if (nodeClass?.contains("Button", ignoreCase = true) == true || 
                nodeClass?.contains("ImageButton", ignoreCase = true) == true) {
                
                if ((nodeText?.contains(text) == true || nodeDesc?.contains(text) == true) && 
                    node.isClickable) {
                    Log.d(TAG, "Found button class match: $nodeClass with text '$nodeText' for digit $text")
                    return node
                }
            }
            
            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findButtonByClass(child, text)
                    if (result != null) return result
                } finally {
                    if (child != node) child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in button class search", e)
        }
        return null
    }
    
    /**
     * Log the complete node hierarchy to help debug what's available
     */
    private fun logNodeHierarchy(node: AccessibilityNodeInfo, depth: Int, targetDigit: String) {
        if (depth > 5) return // Increased depth for better debugging
        
        try {
            val indent = "  ".repeat(depth)
            val nodeText = node.text?.toString() ?: "null"
            val nodeDesc = node.contentDescription?.toString() ?: "null"
            val nodeClass = node.className?.toString() ?: "null"
            val resourceId = node.viewIdResourceName ?: "null"
            val isClickable = node.isClickable
            val isEnabled = node.isEnabled
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            // Log ALL nodes for complete debugging
            Log.d(TAG, "${indent}Node[$depth]: class=$nodeClass")
            Log.d(TAG, "${indent}  text='$nodeText', desc='$nodeDesc'")
            Log.d(TAG, "${indent}  resourceId='$resourceId'")
            Log.d(TAG, "${indent}  clickable=$isClickable, enabled=$isEnabled")
            Log.d(TAG, "${indent}  bounds=$bounds")
            Log.d(TAG, "${indent}  childCount=${node.childCount}")
            
            // Highlight potential matches
            if (nodeText.contains(targetDigit) || nodeDesc.contains(targetDigit) || 
                resourceId.contains(targetDigit)) {
                Log.w(TAG, "${indent}*** POTENTIAL MATCH FOR '$targetDigit' ***")
            }
            
            // Recurse into children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    logNodeHierarchy(child, depth + 1, targetDigit)
                } finally {
                    if (child != node) {
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging node hierarchy at depth $depth", e)
        }
    }
    
    /**
     * Debug method to dump ALL accessibility nodes (call this manually for testing)
     */
    fun debugDumpAllNodes() {
        Log.d(TAG, "\n\n=== DEBUG: DUMPING ALL ACCESSIBILITY NODES ===")
        
        val rootNode = accessibilityService.rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "No root node available for debugging")
            return
        }
        
        try {
            Log.d(TAG, "Root package: ${rootNode.packageName}")
            logCompleteHierarchy(rootNode, 0)
        } finally {
            rootNode.recycle()
        }
        
        Log.d(TAG, "=== END DEBUG DUMP ===\n\n")
    }
    
    private fun logCompleteHierarchy(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 6) return
        
        try {
            val indent = "  ".repeat(depth)
            val nodeText = node.text?.toString() ?: ""
            val nodeDesc = node.contentDescription?.toString() ?: ""
            val nodeClass = node.className?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            
            Log.d(TAG, "${indent}[$depth] $nodeClass")
            if (nodeText.isNotEmpty()) Log.d(TAG, "${indent}    text: '$nodeText'")
            if (nodeDesc.isNotEmpty()) Log.d(TAG, "${indent}    desc: '$nodeDesc'")
            if (resourceId.isNotEmpty()) Log.d(TAG, "${indent}    id: '$resourceId'")
            if (node.isClickable) Log.d(TAG, "${indent}    *** CLICKABLE ***")
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    logCompleteHierarchy(child, depth + 1)
                } finally {
                    if (child != node) child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in complete hierarchy dump", e)
        }
    }
    
    /**
     * Check if accessibility node is still valid
     */
    private fun isNodeValid(node: AccessibilityNodeInfo): Boolean {
        return try {
            // Try to access a property to verify node is valid
            node.className != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Inject text directly into the input field
     */
    private fun injectTextDirectly(node: AccessibilityNodeInfo, digit: String): Boolean {
        try {
            val currentText = getCurrentText(node)
            val newText = currentText + digit
            
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting text directly", e)
            return false
        }
    }
    
    /**
     * Get current text from input field
     */
    private fun getCurrentText(node: AccessibilityNodeInfo): String {
        return try {
            node.text?.toString() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current text", e)
            ""
        }
    }
    
    /**
     * Clear all digits from the input field
     */
    fun clearDigits(): Boolean {
        return try {
            val detectorService = DialPadDetectorService.instance
            if (detectorService == null) {
                Log.e(TAG, "Accessibility service not available")
                return false
            }
            
            val numberField = detectorService.getCurrentInputField()
            if (numberField == null) {
                Log.e(TAG, "Number input field not found")
                return false
            }
            
            if (!isNodeValid(numberField)) {
                Log.e(TAG, "Number field node is no longer valid")
                return false
            }
            
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    ""
                )
            }
            numberField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing digits", e)
            false
        }
    }
    

    
    /**
     * Delete the last digit
     */
    fun deleteLastDigit(): Boolean {
        Log.d(TAG, "=== DELETE LAST DIGIT ===")
        
        return try {
            val detectorService = DialPadDetectorService.instance
            if (detectorService == null) {
                Log.e(TAG, "Accessibility service not available")
                return false
            }
            
            // Method 1: Try to find and click the backspace button
            if (clickBackspaceButton()) {
                Log.d(TAG, "✅ Deleted via backspace button")
                return true
            }
            
            // Method 2: Fallback to text manipulation
            val numberField = detectorService.getCurrentInputField()
            if (numberField == null) {
                Log.e(TAG, "Number input field not found")
                return false
            }
            
            if (!isNodeValid(numberField)) {
                Log.e(TAG, "Number field node is no longer valid")
                return false
            }
            
            val currentText = getCurrentText(numberField)
            if (currentText.isEmpty()) {
                Log.d(TAG, "Number field already empty")
                return true
            }
            
            val newText = currentText.dropLast(1)
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            val success = numberField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Log.d(TAG, "✅ Deleted via text manipulation: $success")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting last digit", e)
            false
        }
    }
    
    /**
     * Click the backspace/delete button in the dialer
     */
    private fun clickBackspaceButton(): Boolean {
        val rootNode = accessibilityService.rootInActiveWindow ?: return false
        
        try {
            // Look for backspace button by resource ID or content description
            val backspacePatterns = listOf(
                "delete", "backspace", "erase", "clear",
                "dialpad_delete", "delete_button"
            )
            
            val button = findButtonByPatterns(rootNode, backspacePatterns)
            if (button != null && isNodeValid(button)) {
                val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                button.recycle()
                return clicked
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking backspace button", e)
        } finally {
            rootNode.recycle()
        }
        
        return false
    }
    
    /**
     * Click the call button in the dialer using STRICT accessibility rules
     * ONLY clicks the actual dialer FAB, NEVER contact suggestions
     */
    fun clickCallButton(): Boolean {
        Log.d(TAG, "clickCallButton: starting search")
        
        val rootNode = accessibilityService.rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "clickCallButton: no root node available")
            return false
        }
        
        try {
            // Step A: Search by specific resource IDs for dialer FAB
            val fabButton = findCallFabByResourceId(rootNode)
            if (fabButton != null) {
                Log.d(TAG, "✅ Found call FAB by resource ID, clicking...")
                val clicked = fabButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                fabButton.recycle()
                Log.d(TAG, "Call FAB click result: $clicked")
                return clicked
            }
            
            // Step B: Search by contentDescription "call", but AVOID contact lists
            val callButton = findCallButtonByDescription(rootNode)
            if (callButton != null) {
                Log.d(TAG, "✅ Found call button by description, clicking...")
                val clicked = callButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                callButton.recycle()
                Log.d(TAG, "Call button click result: $clicked")
                return clicked
            }
            
            // Step C: Nothing found
            Log.w(TAG, "clickCallButton: no suitable call button found")
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in clickCallButton", e)
            return false
        } finally {
            rootNode.recycle()
        }
    }
    

    
    /**
     * Step A: Find call FAB by specific resource IDs
     */
    private fun findCallFabByResourceId(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        Log.d(TAG, "=== Step A: Searching for call FAB by resource ID ===")
        
        val fabResourceIds = listOf(
            "id/dialpad_floating_action_button",
            "id/floating_action_button", 
            "id/dialpad_fab",
            "id/call_fab",
            "id/call_button"
        )
        
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        
        // Collect all nodes with matching resource IDs
        collectNodesByResourceId(rootNode, fabResourceIds, candidates)
        
        // Find the best candidate (FloatingActionButton preferred)
        for (candidate in candidates) {
            try {
                val resourceId = candidate.viewIdResourceName ?: ""
                val className = candidate.className?.toString() ?: ""
                
                Log.d(TAG, "Found candidate: resourceId=$resourceId, className=$className, clickable=${candidate.isClickable}")
                
                if (candidate.isClickable && 
                    (className.contains("FloatingActionButton", ignoreCase = true) ||
                     className.contains("ImageButton", ignoreCase = true) ||
                     className.contains("Button", ignoreCase = true))) {
                    
                    Log.d(TAG, "✅ Selected FAB candidate: $resourceId ($className)")
                    return candidate
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking candidate", e)
            }
        }
        
        Log.d(TAG, "Step A: No suitable FAB found by resource ID")
        return null
    }
    
    /**
     * Step B: Find call button by content description, avoiding contact lists
     */
    private fun findCallButtonByDescription(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        Log.d(TAG, "=== Step B: Searching for call button by description (avoiding contact lists) ===")
        
        return searchCallButtonRecursive(rootNode, false)
    }
    
    /**
     * Recursive search for call button, tracking if we're inside a contact list
     */
    private fun searchCallButtonRecursive(node: AccessibilityNodeInfo, insideContactList: Boolean): AccessibilityNodeInfo? {
        try {
            val className = node.className?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            
            // Check if this node indicates we're entering a contact list area
            val nowInsideContactList = insideContactList || 
                className.contains("RecyclerView", ignoreCase = true) ||
                className.contains("ListView", ignoreCase = true) ||
                className.contains("ListItem", ignoreCase = true) ||
                resourceId.contains("suggested", ignoreCase = true) ||
                resourceId.contains("contacts", ignoreCase = true) ||
                resourceId.contains("contact_list", ignoreCase = true)
            
            if (nowInsideContactList && !insideContactList) {
                Log.d(TAG, "clickCallButton: skipped node inside contact list (className=$className, resourceId=$resourceId)")
            }
            
            // If we're not inside a contact list, check if this is a call button
            if (!nowInsideContactList && node.isClickable) {
                val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                
                // Must be a button-like class
                if (className.contains("Button", ignoreCase = true) ||
                    className.contains("ImageButton", ignoreCase = true) ||
                    className.contains("FloatingActionButton", ignoreCase = true)) {
                    
                    // Must contain "call" in description or text
                    if (contentDesc.contains("call") || text.contains("call")) {
                        Log.d(TAG, "Found call button candidate:")
                        Log.d(TAG, "  text: '$text'")
                        Log.d(TAG, "  contentDescription: '$contentDesc'")
                        Log.d(TAG, "  className: '$className'")
                        Log.d(TAG, "  parent className: '${getParentClassName(node)}'")
                        
                        return node
                    }
                }
            }
            
            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = searchCallButtonRecursive(child, nowInsideContactList)
                    if (result != null) return result
                } finally {
                    if (child != node) child.recycle()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in recursive call button search", e)
        }
        
        return null
    }
    
    /**
     * Helper to collect nodes by resource ID
     */
    private fun collectNodesByResourceId(node: AccessibilityNodeInfo, targetIds: List<String>, results: MutableList<AccessibilityNodeInfo>) {
        try {
            val resourceId = node.viewIdResourceName
            if (resourceId != null) {
                for (targetId in targetIds) {
                    if (resourceId.endsWith(targetId)) {
                        results.add(node)
                        break
                    }
                }
            }
            
            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    collectNodesByResourceId(child, targetIds, results)
                } finally {
                    // Don't recycle here - we're collecting references
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting nodes by resource ID", e)
        }
    }
    
    /**
     * Helper to get parent class name for debugging
     */
    private fun getParentClassName(node: AccessibilityNodeInfo): String {
        return try {
            node.parent?.className?.toString() ?: "null"
        } catch (e: Exception) {
            "error"
        }
    }

    /**
     * Advanced search for call button with multiple strategies (DEPRECATED - kept for compatibility)
     */
    private fun findCallButtonAdvanced(node: AccessibilityNodeInfo, patterns: List<String>): AccessibilityNodeInfo? {
        Log.d(TAG, "=== Advanced Call Button Search ===")
        
        try {
            // Strategy 1: Resource ID matching
            val resourceId = node.viewIdResourceName?.lowercase() ?: ""
            if (resourceId.isNotEmpty()) {
                for (pattern in patterns) {
                    if (resourceId.contains(pattern)) {
                        if (node.isClickable) {
                            Log.d(TAG, "✅ Found call button by resource ID: $resourceId (pattern: $pattern)")
                            return node
                        }
                    }
                }
            }
            
            // Strategy 2: Content description matching
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (contentDesc.isNotEmpty()) {
                for (pattern in patterns) {
                    if (contentDesc.contains(pattern)) {
                        if (node.isClickable) {
                            Log.d(TAG, "✅ Found call button by content description: $contentDesc (pattern: $pattern)")
                            return node
                        }
                    }
                }
            }
            
            // Strategy 3: Text content matching
            val text = node.text?.toString()?.lowercase() ?: ""
            if (text.isNotEmpty()) {
                for (pattern in patterns) {
                    if (text.contains(pattern)) {
                        if (node.isClickable) {
                            Log.d(TAG, "✅ Found call button by text: $text (pattern: $pattern)")
                            return node
                        }
                    }
                }
            }
            
            // Strategy 4: Class name + visual characteristics (FloatingActionButton is common for call)
            val className = node.className?.toString() ?: ""
            if (className.contains("FloatingActionButton", ignoreCase = true) && node.isClickable) {
                Log.d(TAG, "✅ Found FloatingActionButton (likely call button): $className")
                return node
            }
            
            // Strategy 5: Look for ImageButton with call-related descriptions
            if (className.contains("ImageButton", ignoreCase = true) && node.isClickable) {
                if (contentDesc.contains("call") || contentDesc.contains("dial") || 
                    resourceId.contains("call") || resourceId.contains("dial")) {
                    Log.d(TAG, "✅ Found ImageButton with call indicators: class=$className, desc=$contentDesc, id=$resourceId")
                    return node
                }
            }
            
            // Log current node for debugging
            if (node.isClickable) {
                Log.d(TAG, "Clickable node: class=$className, id=$resourceId, desc=$contentDesc, text=$text")
            }
            
            // Recursively search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findCallButtonAdvanced(child, patterns)
                    if (result != null) return result
                } finally {
                    if (child != node) child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in advanced call button search", e)
        }
        
        return null
    }
    
    /**
     * Find button matching any of the given patterns (kept for backward compatibility)
     */
    private fun findButtonByPatterns(node: AccessibilityNodeInfo, patterns: List<String>): AccessibilityNodeInfo? {
        try {
            val resourceId = node.viewIdResourceName?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            
            // Check if this node matches any pattern
            for (pattern in patterns) {
                if (resourceId.contains(pattern) || contentDesc.contains(pattern) || text.contains(pattern)) {
                    if (node.isClickable) {
                        Log.d(TAG, "Found button matching '$pattern': id=$resourceId, desc=$contentDesc")
                        return node
                    }
                }
            }
            
            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findButtonByPatterns(child, patterns)
                    if (result != null) return result
                } finally {
                    if (child != node) child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding button by patterns", e)
        }
        
        return null
    }
}
