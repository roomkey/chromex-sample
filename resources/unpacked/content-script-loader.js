var runFile = function(src) {
  return new Promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if(xhr.readyState == 4) {
        // console.log(xhr.responseText);
        // WARNING! Might be evaluating an evil script!
        eval(xhr.responseText);
        resolve();
      }
    }
    xhr.open("GET", chrome.extension.getURL(src), true);
    xhr.send();
  });
};

runFile('compiled/content_script/chromex-sample.js').then(function() {
  console.log('deps loaded');
  runFile('content-script.js');
});
