package com.slax.reader.const

const val JS_BRIDGE_NAME = "NativeBridge"

const val HEIGHT_MONITOR_SCRIPT: String = """
    (function() {
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
        
        var images = document.getElementsByTagName('img');
        for (var i = 0; i < images.length; i++) {            
            images[i].style = ''
            
            images[i].addEventListener('click', function(event) {
                // 获取所有图片的URL
                var allImageUrls = [];
                for (var j = 0; j < images.length; j++) {
                    if (images[j].src) {
                        allImageUrls.push(images[j].src);
                    }
                }

                var payload = JSON.stringify({
                    type: 'imageClick',
                    src: event.target.src,
                    allImages: allImageUrls
                });

                if (window.NativeBridge && window.NativeBridge.postMessage) {
                    window.NativeBridge.postMessage(payload);
                } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.NativeBridge) {
                    window.webkit.messageHandlers.NativeBridge.postMessage(payload);
                }
            });
        }
    })();
"""
