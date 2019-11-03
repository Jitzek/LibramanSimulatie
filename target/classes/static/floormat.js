/**
 * adds texture to the floor
 * color, displacement and roughness for the floor
 */
function floormat(){
    var floorMat;
floorMat = new THREE.MeshStandardMaterial({
  roughness: 0.6,
  color: 0xffffff,
  metalness: 0.1,
  bumpScale: 0.001,
  side: THREE.DoubleSide
});
var textureLoader = new THREE.TextureLoader();
textureLoader.load("textures/Concrete_009_COLOR.jpg", function(map) {
  map.wrapS = THREE.RepeatWrapping;
  map.wrapT = THREE.RepeatWrapping;
  map.anisotropy = 4;
  map.repeat.set(30, 20);
  floorMat.map = map;
  floorMat.needsUpdate = true;
});
textureLoader.load("textures/Concrete_009_DISP.jpg", function(map) {
  map.wrapS = THREE.RepeatWrapping;
  map.wrapT = THREE.RepeatWrapping;
  map.anisotropy = 4;
  map.repeat.set(30, 20);
  floorMat.bumpMap = map;
  floorMat.needsUpdate = true;
});
textureLoader.load("textures/Concrete_009_ROUGH.jpg", function(map) {
  map.wrapS = THREE.RepeatWrapping;
  map.wrapT = THREE.RepeatWrapping;
  map.anisotropy = 4;
  map.repeat.set(30, 20);
  floorMat.roughnessMap = map;
  floorMat.needsUpdate = true;
});
return floorMat;
}
