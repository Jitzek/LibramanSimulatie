function parseCommand(input = "") {
  return JSON.parse(input);
}

var socket;

window.onload = function() {
  var camera, scene, renderer;
  var cameraControls;
  var worldObjects = {};
  var planewidth = 30;
  var planeheight = 30;
  var topcornerx = planewidth / 2 - 0.6 - 2;
  var topcornery = planeheight / 2 - 1.6 - 2;
  var bottomcornerx = -(planewidth / 2 - 0.6) + 2;
  var bottomcornery = -(planeheight / 2 - 1.6) + 2;
  var xracks = Math.floor((planewidth - 2) / 5);
  var zracks = Math.floor((planeheight - 5) / 4);
  var toRobot1;
  var toRobot2;
  let models = [];
  var robot1;
  var robot2;
  var counterrobot = 0;

  /**
   * adds sound to the robot
   * @param {camera thats in the scene} camera
   * count is there so there is just one instance of the sound
   * Refdistance for volume decrease over distance
   * returns sound to the robot
   */
  function robotSound(camera) {
    var listener = new THREE.AudioListener();
    camera.add(listener);
    var sound = new THREE.PositionalAudio(listener);
    var audioLoader = new THREE.AudioLoader();
    var count = 0;
    audioLoader.load("sounds/untitled.ogg", function(buffer) {
      sound.setBuffer(buffer);
      sound.setRefDistance(0.3);
      sound.setVolume(1); //don't go over a value of 10, just don't do it
      sound.setLoop(true);
      if (count == 0) {
        sound.play();
        count++;
      }
    });
    return sound;
  }

  var loader = new THREE.GLTFLoader();
  function rack(x, y, z) {
    loader.load(
      "models/rackding.gltf", //load the model you want to use
      function(gltf) {
        gltf.scene.scale.set(0.023, 0.023, 0.023); //if model is to big or too small, change this value
        gltf.scene.position.x = x;
        gltf.scene.position.y = y;
        gltf.scene.position.z = z;
        scene.add(gltf.scene);
        var reset = z;
        //loops to put boxes on each rack
        //width rack = 1.2
        //width doublerack + path = 2.4 + 2.4 = 4.8
        //height rack = 3.5

        for (let i = 0; i < 5; i++) {
          for (let i = 0; i < 3; i++) {
            //box(x, y, z + 0.4);
            z -= 1;
          }
          y += 0.9;
          z = reset;
        }

        gltf.animations; // Array<THREE.AnimationClip>
        gltf.scene; // THREE.Scene
        gltf.scenes; // Array<THREE.Scene>
        gltf.cameras; // Array<THREE.Camera>
        gltf.asset; // Object
      },
      function(xhr) {
        //console.log((xhr.loaded / xhr.total) * 100 + "% loaded");
      },
      function(error) {
        //console.log("An error happened");
      }
    );
  }
  //box needs an x y and z value, this function is uses in rack, but could also be used for other stuff if needed
  function box(x, y, z, uuid) {
    loader.load(
      "models/doazespiegelt.gltf",
      function(gltf) {
        gltf.scene.scale.set(1, 1, 1);
        gltf.scene.position.x = x;
        gltf.scene.position.y = y;
        gltf.scene.position.z = z;
        const root = gltf.scene;
        root.uuid = uuid;
        root.scale.y = -1;
        models.push(root.getObjectByName("Scene"));
        scene.add(root);

        gltf.animations; // Array<THREE.AnimationClip>
        gltf.scene; // THREE.Scene
        gltf.scenes; // Array<THREE.Scene>
        gltf.cameras; // Array<THREE.Camera>
        gltf.asset; // Object
      },
      function(xhr) {
        //console.log((xhr.loaded / xhr.total) * 100 + "% loaded");
      },
      function(error) {
        //console.log("error");
      }
    );
  }
  function truck(x, y, z, uuid) {
    loader.load(
      "models/libratruck.gltf",
      function(gltf) {
        gltf.scene.scale.set(0.07, 0.07, 0.07);
        gltf.scene.position.x = x;
        gltf.scene.position.y = y;
        gltf.scene.position.z = z;
        const root = gltf.scene;
        root.uuid = uuid;
        models.push(root.getObjectByName("Scene"));
        scene.add(root);
      },
      function(xhr) {},
      function(error) {
        console.log(error);
      }
    );
  }

  /**
   * spawns in filled racks
   * see world.java.addRacks() for more info
   */
  function spawn() {
    var z = bottomcornery;
    var counter = 0; //counter is used to make a big opening in the middle

    for (let i = 0; i < zracks; i++) {
      var x = bottomcornerx;
      for (let i = 0; i < xracks; i++) {
        if (x <= topcornerx && z < topcornery) {
          rack(x, 0, z);
          //rack(x + 1.5, 0, z);
          x += 5;
        }
      }
      counter++;
      if (counter == 3) {
        z += 8.5;
        counter = 0;
      } else {
        z += 3.5;
      }
    }
  }

  function init() {
    camera = new THREE.PerspectiveCamera(
      70,
      window.innerWidth / window.innerHeight,
      1,
      1000
    );

    cameraControls = new THREE.OrbitControls(camera);
    camera.position.z = -15;
    camera.position.y = 5;
    camera.position.x = 25;
    //keeps the camera up
    camera.up = new THREE.Vector3(0, 1, 0);

    cameraControls.update();
    scene = new THREE.Scene();

    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.physicallyCorrectLights = true;
    renderer.autoClearColor = false;
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.setSize(window.innerWidth, window.innerHeight + 5);
    document.body.appendChild(renderer.domElement);
    window.addEventListener("resize", onWindowResize, false);

    var geometry = new THREE.PlaneGeometry(planewidth, planeheight);
    var geometry2 = new THREE.PlaneGeometry(planewidth, 10);
    var geometry3 = new THREE.PlaneGeometry(planewidth, 10);
    var geometry4 = new THREE.PlaneGeometry(planewidth - 15, 5);

    /**
     * adds texture to the floor
     * color, displacement and roughness for the floor
     */
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
    /**
     * color for brick wall texture
     */
    var texture = new THREE.TextureLoader().load("textures/wall_color.jpg");
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    texture.repeat.set(5, 3);
    var material = new THREE.MeshBasicMaterial({
      map: texture,
      side: THREE.DoubleSide
    });
        var libraman = new THREE.TextureLoader().load("textures/Libraman.jpg");
        libraman.wrapS = THREE.RepeatWrapping;
        libraman.wrapT = THREE.RepeatWrapping;
        var material2 = new THREE.MeshBasicMaterial({
          map: libraman,
          side: THREE.DoubleSide
        });

    /**
     * adds 3 planes to the scene
     * floor with concrete texture
     * walls with brick texture
     */
    var plane = new THREE.Mesh(geometry, floorMat);
    plane.rotation.x = Math.PI / 2.0;
    plane.position.x = 0;
    plane.position.z = 0;
    scene.add(plane);
    var plane2 = new THREE.Mesh(geometry2, material);
    plane2.position.z = planewidth / 2;
    plane2.position.y = planewidth / 6;
    scene.add(plane2);
    var plane3 = new THREE.Mesh(geometry3, material);
    plane3.position.y = planewidth / 6;
    plane3.position.x = -(planewidth / 2);
    plane3.rotation.y = Math.PI / 2.0;
    scene.add(plane3);
    var plane4 = new THREE.Mesh(geometry4, material2);
    plane4.position.z = planewidth / 2 - 0.1;
    plane4.position.y = planewidth / 6;
    plane4.scale.x = -1;
    scene.add(plane4);


    spawn();

    /**
     * adds light to the scene
     */
    const color = 0xffffff;
    const intensity = 1;
    const light = new THREE.PointLight(color, intensity);
    const light2 = new THREE.PointLight(color, intensity);
    light.power = 600;
    light2.power = 600;
    light.decay = 1;
    light2.decay = 1;
    light.distance = Infinity;
    light2.distance = Infinity;
    light.position.set(20, 25, 0);
    light2.position.set(40, 15, 25);
    var sphereSize = 1;
    var pointLightHelper = new THREE.PointLightHelper(light2, sphereSize);
    var light3 = new THREE.HemisphereLight(color, color, 1);
    scene.add(light3);
    scene.add(pointLightHelper);
    scene.add(light);
    scene.add(light2);
  }

  function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  }

  const bgScene = new THREE.Scene();
  let bgMesh;
  {
    const loader = new THREE.TextureLoader();
    const texture = loader.load("textures/de_dust.png");
    texture.magFilter = THREE.LinearFilter;
    texture.minFilter = THREE.LinearFilter;

    const shader = THREE.ShaderLib.equirect;
    const material = new THREE.ShaderMaterial({
      fragmentShader: shader.fragmentShader,
      vertexShader: shader.vertexShader,
      uniforms: shader.uniforms,
      depthWrite: false,
      side: THREE.BackSide
    });
    material.uniforms.tEquirect.value = texture;
    const plane = new THREE.BoxBufferGeometry(10, 10, 10);
    bgMesh = new THREE.Mesh(plane, material);
    bgScene.add(bgMesh);
  }
  function animate() {
    requestAnimationFrame(animate);
    cameraControls.update();
    bgMesh.position.copy(camera.position);
    renderer.render(bgScene, camera);
    renderer.render(scene, camera);
  }

  /*
   * Hier wordt de socketcommunicatie geregeld. Er wordt een nieuwe websocket aangemaakt voor het webadres dat we in
   * de server geconfigureerd hebben als connectiepunt (/connectToSimulation). Op de socket wordt een .onmessage
   * functie geregistreerd waarmee binnenkomende berichten worden afgehandeld.
   */
  //socket = new WebSocket("ws://" + window.location.hostname + ":" + window.location.port + "/connectToSimulation");
  socket = new WebSocket("ws://localhost:8080" + "/connectToSimulation");
  socket.onmessage = function(event) {
    //Hier wordt het commando dat vanuit de server wordt gegeven uit elkaar gehaald
    var command = parseCommand(event.data);

    //Wanneer het commando is "object_update", dan wordt deze code uitgevoerd. Bekijk ook de servercode om dit goed te begrijpen.
    if (command.command == "object_update") {
      //Wanneer het object dat moet worden geupdate nog niet bestaat (komt niet voor in de lijst met worldObjects op de client),
      //dan wordt het 3D model eerst aangemaakt in de 3D wereld.
      if (Object.keys(worldObjects).indexOf(command.parameters.uuid) < 0) {
        //Wanneer het object een robot is, wordt de code hieronder uitgevoerd.
        if (command.parameters.type == "robot") {
          var x = 0.9; // X size
          var y = 0.3; // Y size
          var z = 0.9; // Z size
          var geometry = new THREE.BoxGeometry(x, y, z);
          var material = new THREE.MeshBasicMaterial({ color: 0x99ff00 });
          material.transparent = true;
          material.opacity = 0.2;
          var collisionBox = new THREE.Mesh(geometry, material);

          var geometry = new THREE.BoxGeometry(0.9, 0.3, 0.9);
          var cubeMaterials = [
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load("textures/robot_side_1.png"),
              side: THREE.DoubleSide
            }), //LEFT
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load("textures/robot_side_1.png"),
              side: THREE.DoubleSide
            }), //RIGHT
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load("textures/robot_top_1.png"),
              side: THREE.DoubleSide
            }), //TOP
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load("textures/robot_bottom.png"),
              side: THREE.DoubleSide
            }), //BOTTOM
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load("textures/robot_front_1.png"),
              side: THREE.DoubleSide
            }), //FRONT
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load("textures/robot_front_1.png"),
              side: THREE.DoubleSide
            }) //BACK
          ];

          /**
           * adds sound to the robot
           *
           */
          var sound = robotSound(camera);
          var material = new THREE.MeshFaceMaterial(cubeMaterials);
          var robot = new THREE.Mesh(geometry, material);
          robot.position.y = 0.15;
          robot.add(sound);

          var group = new THREE.Group();
          group.add(robot);
          //group.add(collisionBox);

          //group.add(rack);
          //group.add(box);
          scene.add(group);

          document.addEventListener("keydown", onKeyDown, false);
          worldObjects[command.parameters.uuid] = group;
        }
        if (command.parameters.type == "rack") {
          var x = 1.2; // X size
          var y = 5.0; // Y size
          var z = 4.0; // Z size
          var geometry = new THREE.BoxGeometry(x, y, z);
          var material = new THREE.MeshBasicMaterial({ color: 0x99ff00 });
          material.transparent = true;
          material.opacity = 0.2;
          var group = new THREE.Group();

          var collisionBox = new THREE.Mesh(geometry, material);
          collisionBox.position.y = y / 2;
          //group.add(collisionBox);

          scene.add(group);
          worldObjects[command.parameters.uuid] = group;
        }
        if (command.parameters.type == "truck") {
          var geometry = new THREE.BoxGeometry(
            command.parameters.sizeX,
            command.parameters.sizeY,
            command.parameters.sizeZ
          );
          var material = new THREE.MeshBasicMaterial({ color: 0x00ff00 });
          material.transparent = true;
          material.opacity = 0.2;
          var group = new THREE.Group();
          group.name = "truck";

          var collisionBox = new THREE.Mesh(geometry, material);
          collisionBox.position.y = command.parameters.sizeY / 2;
          truck(
            command.parameters.x,
            command.parameters.y,
            command.parameters.z,
            command.parameters.uuid
          );
          //group.add(collisionBox);

          scene.add(group);
          worldObjects[command.parameters.uuid] = group;
        }
        if (command.parameters.type == "item") {
          var x = 1.2; // X size
          var y = 0.75; // Y size
          var z = 0.75; // Z size
          var geometry = new THREE.BoxGeometry(x, y, z);
          var material = new THREE.MeshBasicMaterial({ color: 0x0080ff });
          material.transparent = true;
          material.opacity = 0.9;
          var group = new THREE.Group();

          var item = new THREE.Mesh(geometry, material);
          //group.add(item);

          box(
            command.parameters.x,
            command.parameters.y,
            command.parameters.z - z / 2,
            command.parameters.uuid
          );
          item.position.y = y / 2;

          scene.add(group);
          worldObjects[command.parameters.uuid] = group;
        }
        if (command.parameters.type == "obstacle") {
          /*var geometry = new THREE.BoxGeometry( 1.2, 5.0, 11.0);
          var material = new THREE.MeshBasicMaterial({ color: 0x99ff00 });
          var group = new THREE.Group();

          var obstacle = new THREE.Mesh(geometry, material);
          group.add(obstacle);

          scene.add(group);
          worldObjects[command.parameters.uuid] = group;*/
        }
      }

      /*
       * Deze code wordt elke update uitgevoerd. Het update alle positiegegevens van het 3D object.
       */
      var object = worldObjects[command.parameters.uuid];

      object.position.x = command.parameters.x;
      object.position.y = command.parameters.y;
      object.position.z = command.parameters.z;

      if (models) {
        for (const model of models) {
          if (model.uuid === command.parameters.uuid) {
            model.position.x = command.parameters.x;
            model.position.y = command.parameters.y;
            model.position.z = command.parameters.z;

            model.rotation.x = command.parameters.rotationX;
            model.rotation.y = command.parameters.rotationY;
            model.rotation.z = command.parameters.rotationZ;
          }
        }
      }

      object.rotation.x = command.parameters.rotationX;
      object.rotation.y = command.parameters.rotationY;
      object.rotation.z = command.parameters.rotationZ;
      if (command.parameters.type == "robot") {
        if (counterrobot == 0) {
          robot1 = command.parameters.uuid;
          counterrobot++;
        } else if (counterrobot == 1) {
          robot2 = command.parameters.uuid;
          counterrobot++;
        }
      }

      // If the positions of an object equal the positions defined in World.java (dump[])
      if (
        object.position.x == -1000 &&
        object.position.y == -1000 &&
        object.position.z == -1000
      ) {
        // Delete Object
        scene.remove(object);
        if (models) {
          for (const model of models) {
            if (model.uuid === command.parameters.uuid) {
              // Remove model
              scene.remove(model);
            }
          }
        }
      }

      /**
       * function to move the camera to the robot
       * keycode 49 is 1(not numpad)
       * keycode 50 is 2(still not numpad)
       */

      function onKeyDown(event) {
        var keyCode = event.which;
        if (keyCode == "49") {
          toRobot1 = true;
          toRobot2 = false;
        } else if (keyCode == "50") {
          toRobot2 = true;
          toRobot1 = false;
        } else if (keyCode == "27") {
          toRobot1 = false;
          toRobot2 = false;
          camera.position.z = -15;
          camera.position.y = 5;
          camera.position.x = 25;
        }
      }
      /**
       * if toRobot == true, camera moves to robot.
       */
      if (toRobot1) {
        if (command.parameters.uuid == robot1) {
          console.log(command.parameters.action);
          if (command.parameters.action == "z-") {
            camera.position.x = object.position.x;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z + 2;
          } else if (command.parameters.action == "z+") {
            camera.position.x = object.position.x;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z - 2;
          } else if (command.parameters.action == "x-") {
            camera.position.x = object.position.x + 2;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z;
          } else if (command.parameters.action == "x+") {
            camera.position.x = object.position.x - 2;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z;
          }
          cameraControls.target.set(
            object.position.x,
            object.position.y,
            object.position.z
          );
        }
      }
      if (toRobot2) {
        if (command.parameters.uuid == robot2) {
          console.log(command.parameters.action);
          if (command.parameters.action == "z-") {
            camera.position.x = object.position.x;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z + 2;
          } else if (command.parameters.action == "z+") {
            camera.position.x = object.position.x;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z - 2;
          } else if (command.parameters.action == "x-") {
            camera.position.x = object.position.x + 2;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z;
          } else if (command.parameters.action == "x+") {
            camera.position.x = object.position.x - 2;
            camera.position.y = object.position.y + 2;
            camera.position.z = object.position.z;
          }
          cameraControls.target.set(
            object.position.x,
            object.position.y,
            object.position.z
          );
        }
      }
    }
  };

  init();
  animate();
};
