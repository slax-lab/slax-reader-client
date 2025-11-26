package com.slax.reader.const

const val JS_BRIDGE_NAME = "NativeBridge"

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
                
        
    })();
"""
