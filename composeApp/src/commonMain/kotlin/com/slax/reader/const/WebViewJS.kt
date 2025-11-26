package com.slax.reader.const

const val JS_BRIDGE_NAME = "NativeBridge"

// Script to load slax-selection-bridge.js (Android)
const val LOAD_SELECTION_BRIDGE_SCRIPT_ANDROID: String = """
    (function() {
        var script = document.createElement('script');
        script.src = 'file:///android_asset/js/slax-selection-bridge.js';
        script.onload = function() {
            console.log('[SlaxBridge] Selection bridge loaded successfully');

            // Initialize the bridge after a short delay to ensure DOM is ready
            setTimeout(function() {
                if (typeof window.initializeBridge === 'function') {
                    console.log('[SlaxBridge] Calling initializeBridge...');
                    var success = window.initializeBridge(1);
                    if (success) {
                        console.log('[SlaxBridge] Bridge initialized successfully from script load');
                    } else {
                        console.error('[SlaxBridge] Bridge initialization failed');
                    }
                } else {
                    console.error('[SlaxBridge] initializeBridge function not found');
                }
            }, 100);
        };
        script.onerror = function() {
            console.error('[SlaxBridge] Failed to load selection bridge');
        };
        document.head.appendChild(script);
    })();
"""

// Script to load slax-selection-bridge.js (iOS - will be injected as WKUserScript)
const val LOAD_SELECTION_BRIDGE_SCRIPT_IOS: String = """
    // Placeholder - iOS loads via WKUserScript directly
"""

// Alias for backward compatibility
const val LOAD_SELECTION_BRIDGE_SCRIPT = LOAD_SELECTION_BRIDGE_SCRIPT_ANDROID

const val INJECTED_SCRIPT: String = """
    (function() {
        function postToNativeBridge(payload) {
            const message = JSON.stringify(payload);

            if (window.NativeBridge?.postMessage) {
                window.NativeBridge.postMessage(message);
                return true;
            }

            if (window.webkit?.messageHandlers?.NativeBridge) {
                window.webkit.messageHandlers.NativeBridge.postMessage(message);
                return true;
            }

            console.warn('Native bridge not available');
            return false;
        }

        function getContentHeight() {
            return Math.max(
                document.body.scrollHeight,
                document.body.offsetHeight,
                document.documentElement.clientHeight,
                document.documentElement.scrollHeight,
                document.documentElement.offsetHeight
            );
        }

        var anchors = document.getElementsByTagName('a')
        for (var i = 0; i < anchors.length; i++) {
            anchors[i].addEventListener('click', (event) => {
              event.preventDefault();
            });
        }

        function getImageUrl(element) {
            const tagName = element.tagName.toLowerCase();

            if (tagName === 'img') {
                return element.currentSrc || element.src || '';
            }

            if (tagName === 'image') {
                return element.href?.baseVal ||
                       element.getAttribute('href') ||
                       element.getAttribute('xlink:href') ||
                       '';
            }

            return '';
        }

        const images = document.querySelectorAll('img, image');
        images.forEach(img => {
            img.style.cssText = '';

            img.addEventListener('click', (event) => {
                const allImageUrls = Array.from(images)
                    .map(getImageUrl)
                    .filter(url => url);

                const clickedImageUrl = getImageUrl(event.currentTarget);

                postToNativeBridge({
                    type: 'imageClick',
                    src: clickedImageUrl,
                    allImages: allImageUrls,
                    index: allImageUrls.indexOf(clickedImageUrl)
                });
            });
        });

        // Initialize selection bridge adapter
        window.SlaxBridge = {
            log: function(level, message) {
                console.log('[SlaxBridge][' + level + ']', message);
            },
            onTextSelected: function(data, position) {
                postToNativeBridge({
                    type: 'textSelected',
                    data: data,
                    position: position
                });
            },
            onMarkClicked: function(markId, data, position) {
                postToNativeBridge({
                    type: 'markClicked',
                    markId: markId,
                    data: data,
                    position: position
                });
            },
            onBridgeInitialized: function() {
                postToNativeBridge({
                    type: 'bridgeInitialized'
                });
            },
            onMarkRendered: function(markId, success) {
                postToNativeBridge({
                    type: 'markRendered',
                    markId: markId,
                    success: success
                });
            },
            onError: function(error) {
                postToNativeBridge({
                    type: 'error',
                    error: error
                });
            }
        };


    })();
"""
