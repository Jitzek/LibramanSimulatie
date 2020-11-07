/**
 * Handels audio of robot
 */
let firstTime = true;
function robotSound(camera) {
  const listener = new THREE.AudioListener();
  const sound = new THREE.PositionalAudio(listener);
  const audioLoader = new THREE.AudioLoader();
  camera.add(listener);
  audioLoader.load("sounds/untitled.ogg", function (buffer) {
    sound.setBuffer(buffer);
    sound.setRefDistance(0.1);
    sound.setVolume(0.3);
    sound.setLoop(true);
    if (firstTime) {
      sound.play();
      firstTime = false;
    }
  });
  return sound;
}
