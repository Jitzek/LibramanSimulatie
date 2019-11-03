/**
 * adds sound to the robot
 * @param {camera thats in the scene} camera
 * count is there so there is just one instance of the sound
 * Refdistance for volume increase/decrease over distance
 * returns sound to the robot
 * sound used is made and edited by us
 */

function robotSound(camera) {
  var listener = new THREE.AudioListener();
  camera.add(listener);
  var sound = new THREE.PositionalAudio(listener);
  var audioLoader = new THREE.AudioLoader();
  var count = 0;
  audioLoader.load("sounds/untitled.ogg", function(buffer) {
    sound.setBuffer(buffer);
    sound.setRefDistance(0.1);
    sound.setVolume(0.3);
    sound.setLoop(true);
    if (count == 0) {
      sound.play();
      count++;
    }
  });
  return sound;
}
