function copyToClipboard(textToCopy) {
  navigator.clipboard.writeText(textToCopy);
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