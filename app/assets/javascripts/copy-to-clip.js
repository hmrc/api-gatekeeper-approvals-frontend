function copyToClipboard(textToCopy) {
  // navigator clipboard api needs a secure context (https)
  if (navigator.clipboard && window.isSecureContext) {
    // navigator clipboard api method'
    return navigator.clipboard.writeText(textToCopy);
  } else {
    // text area method
    var textArea = document.createElement("textarea");
    textArea.value = textToCopy;
    // make the textarea out of viewport
    textArea.style.position = "fixed";
    textArea.style.left = "-999999px";
    textArea.style.top = "-999999px";
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
      var successful = document.execCommand('copy');
    } catch (e) {
      // allow failure - still want to remove textArea
      // test if we should even display the button later
    }

    textArea.remove();
  }
}

function initCopyTextOnClick(clickedElementId, textSourceId) {
  if (clickedElementId != null && textSourceId != null) {
    var clickedElement = document.getElementById(clickedElementId)
    var textSource = document.getElementById(textSourceId)
    if (clickedElement != null && textSource != null) {
      clickedElement.addEventListener(
        'click',
        function () {
          var text = textSource.textContent
          copyToClipboard(text)
        },
        false
      )
    }
  }
}