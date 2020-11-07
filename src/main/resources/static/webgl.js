function parseCommand(input = "") {
  return JSON.parse(input);
}

window.onload = function() {
  let camera, scene, renderer;
  let cameraControls;
  let worldObjects = {};
  const planewidth = 30;
  const planeheight = 30;
  const topcornerx = planewidth / 2 - 0.6 - 2;
  const topcornery = planeheight / 2 - 1.6 - 2;
  const bottomcornerx = -(planewidth / 2 - 0.6) + 2;
  const bottomcornery = -(planeheight / 2 - 1.6) + 2;
  const xracks = Math.floor((planewidth - 2) / 5);
  const zracks = Math.floor((planeheight - 5) / 4);
  let toRobot1;
  let toRobot2;
  let models = [];
  let robot1;
  let robot2;
  let counterrobot = 0;

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
    camera.up = new THREE.Vector3(0, 1, 0);

    cameraControls.update();
    scene = new THREE.Scene();

    renderer = new THREE.WebGLRenderer({
      antialias: true
    });
    renderer.physicallyCorrectLights = true;
    renderer.autoClearColor = false;
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.setSize(
      window.innerWidth,
      window.innerHeight + 5
    );
    document.body.appendChild(renderer.domElement);
    window.addEventListener(
      "resize",
      onWindowResize,
      false
    );

    const floorGeometry = new THREE.PlaneGeometry(
      planewidth,
      planeheight
    );
    const posterWallGeometry = new THREE.PlaneGeometry(
      planewidth,
      10
    );
    const normalWallGeometry = new THREE.PlaneGeometry(
      planewidth,
      10
    );
    const posterGeometry = new THREE.PlaneGeometry(
      planewidth - 15,
      5
    );


    /**
     * color for brick wall texture
     */
    const texture = new THREE.TextureLoader().load(
      "textures/wall_color.jpg"
    );
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    texture.repeat.set(5, 3);
    const wallMaterial = new THREE.MeshBasicMaterial({
      map: texture,
      side: THREE.DoubleSide
    });
    /**
     * image for the poster
     */
    const libraman = new THREE.TextureLoader().load(
      "textures/Libraman.jpg"
    );
    libraman.wrapS = THREE.RepeatWrapping;
    libraman.wrapT = THREE.RepeatWrapping;
    const posterMaterial = new THREE.MeshBasicMaterial({
      map: libraman,
      side: THREE.DoubleSide
    });


    const floorMaterial = new floormat();

    const floorMesh = new THREE.Mesh(floorGeometry, floorMaterial);
    floorMesh.rotation.x = Math.PI / 2.0;
    floorMesh.position.x = 0;
    floorMesh.position.z = 0;
    scene.add(floorMesh);

    const posterWallMesh = new THREE.Mesh(posterWallGeometry, wallMaterial);
    posterWallMesh.position.z = planewidth / 2;
    posterWallMesh.position.y = planewidth / 6;
    scene.add(posterWallMesh);

    const normalWallMesh = new THREE.Mesh(normalWallGeometry, wallMaterial);
    normalWallMesh.position.y = planewidth / 6;
    normalWallMesh.position.x = -(planewidth / 2);
    normalWallMesh.rotation.y = Math.PI / 2.0;
    scene.add(normalWallMesh);

    const posterMesh = new THREE.Mesh(
      posterGeometry,
      posterMaterial
    );
    posterMesh.position.z = planewidth / 2 - 0.1;
    posterMesh.position.y = planewidth / 6;
    posterMesh.scale.x = -1;
    scene.add(posterMesh);

    spawn();

    /**
     * adds light to the scene
     * 2 pointlights to light up the scene and create some shadows
     * hemispherelight to bring some light to the dark spaces
     */
    const color = 0xffffff;
    const intensity = 1;
    const light = new THREE.PointLight(
      color,
      intensity
    );
    const light2 = new THREE.PointLight(
      color,
      intensity
    );
    light.power = 600;
    light2.power = 600;
    light.decay = 1;
    light2.decay = 1;
    light.distance = Infinity;
    light2.distance = Infinity;
    light.position.set(20, 25, 0);
    light2.position.set(40, 15, 25);

    const light3 = new THREE.HemisphereLight(
      color,
      color,
      1
    );
    scene.add(light3);
    scene.add(light);
    scene.add(light2);
  }

  function onWindowResize() {
    camera.aspect =
      window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(
      window.innerWidth,
      window.innerHeight
    );
  }

  /**
   * skybox for the scene, not really fitting, but points are points
   * if the near or far clips with the skybox, adjust boxbuffergeometry()
   */
  const bgScene = new THREE.Scene();
  let bgMesh;
  {
    const loader = new THREE.TextureLoader();
    const texture = loader.load(
      "textures/dust2.png"
    );
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
    const plane = new THREE.BoxBufferGeometry(
      10,
      10,
      10
    );
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
  const socket = new WebSocket(
    "ws://localhost:8080" + "/connectToSimulation"
  );
  socket.addEventListener('message', function (event) {
    //Hier wordt het commando dat vanuit de server wordt gegeven uit elkaar gehaald
    const command = parseCommand(event.data);

    //Wanneer het commando is "object_update", dan wordt deze code uitgevoerd. Bekijk ook de servercode om dit goed te begrijpen.
    if (command.command == "object_update") {
      //Wanneer het object dat moet worden geupdate nog niet bestaat (komt niet voor in de lijst met worldObjects op de client),
      //dan wordt het 3D model eerst aangemaakt in de 3D wereld.
      if (
        Object.keys(worldObjects).indexOf(
          command.parameters.uuid
        ) < 0
      ) {
        //Wanneer het object een robot is, wordt de code hieronder uitgevoerd.
        if (command.parameters.type == "robot") {
          const robotGeometry = new THREE.BoxBufferGeometry(
            0.9,
            0.3,
            0.9
          );

          const cubeMaterials = [
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load(
                "textures/robot_side_1.png"
              ),
              side: THREE.DoubleSide
            }), //LEFT
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load(
                "textures/robot_side_1.png"
              ),
              side: THREE.DoubleSide
            }), //RIGHT
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load(
                "textures/robot_top_1.png"
              ),
              side: THREE.DoubleSide
            }), //TOP
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load(
                "textures/robot_bottom.png"
              ),
              side: THREE.DoubleSide
            }), //BOTTOM
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load(
                "textures/robot_front_1.png"
              ),
              side: THREE.DoubleSide
            }), //FRONT
            new THREE.MeshBasicMaterial({
              map: new THREE.TextureLoader().load(
                "textures/robot_front_1.png"
              ),
              side: THREE.DoubleSide
            }) //BACK
          ];

          const sound = new robotSound(camera);
          const robot = new THREE.Mesh(
            robotGeometry,
            cubeMaterials
          );
          robot.position.y = 0.15;
          robot.add(sound);

          const robotGroup = new THREE.Group();
          robotGroup.add(robot);
          //group.add(rack);
          //group.add(box);
          scene.add(robotGroup);

          document.addEventListener(
            "keydown",
            onKeyDown,
            false
          );
          worldObjects[
            command.parameters.uuid
          ] = robotGroup;
        }
        /**
         * when server gives back rack
         * *sizes in json should be implemented here, still some errors, so coming soon*
         */
        if (command.parameters.type == "rack") {
          const rackGeometry = new THREE.BoxBufferGeometry(
            1.2,
            5.0,
            4.0
          );
          const rackGroup = new THREE.Group();
          scene.add(rackGroup);
          worldObjects[
            command.parameters.uuid
          ] = rackGroup;
        }
        /**
         * server gives back truck
         * *sizes in json should be implemented here, still some errors, so coming soon*
         */
        if (command.parameters.type == "truck") {
          const truckGeometry = new THREE.BoxBufferGeometry(
            command.parameters.sizeX,
            command.parameters.sizeY,
            command.parameters.sizeZ
          );
          const truckGroup = new THREE.Group();
          truckGroup.name = "truck";

          truck(
            command.parameters.x,
            command.parameters.y,
            command.parameters.z,
            command.parameters.uuid
          );

          scene.add(truckGroup);
          worldObjects[
            command.parameters.uuid
          ] = truckGroup;
        }
        /**
         * server gives back item
         * *sizes in json should be implemented here, still some errors, so coming soon*
         */
        if (command.parameters.type == "item") {
          const x = 1.2; // X size
          const y = 0.75; // Y size
          const z = 0.75; // Z size
          const itemGeometry = new THREE.BoxBufferGeometry(
            1.2,
            0.75,
            0.75
          );
          const itemMaterial = new THREE.MeshBasicMaterial(
            {
              color: 0x0080ff
            }
          );
          itemMaterial.transparent = true;
          itemMaterial.opacity = 0.9;
          const itemGroup = new THREE.Group();

          const item = new THREE.Mesh(
            itemGeometry,
            itemMaterial
          );
          //itemGroup.add(item);

          box(
            command.parameters.x,
            command.parameters.y,
            command.parameters.z - z / 2,
            command.parameters.uuid
          );
          item.position.y = y / 2;

          scene.add(itemGroup);
          worldObjects[
            command.parameters.uuid
          ] = itemGroup;
        }
        if (command.parameters.type == "obstacle") {
        }
      }

      /*
       * Deze code wordt elke update uitgevoerd. Het update alle positiegegevens van het 3D object.
       */
      const object =
        worldObjects[command.parameters.uuid];

      object.position.x = command.parameters.x;
      object.position.y = command.parameters.y;
      object.position.z = command.parameters.z;

      /**
       * updates the truck and box position for the animations
       */
      if (models) {
        for (const model of models) {
          if (
            model.uuid === command.parameters.uuid
          ) {
            model.position.x = command.parameters.x;
            model.position.y = command.parameters.y;
            model.position.z = command.parameters.z;

            model.rotation.x =
              command.parameters.rotationX;
            model.rotation.y =
              command.parameters.rotationY;
            model.rotation.z =
              command.parameters.rotationZ;
          }
        }
      }

      object.rotation.x =
        command.parameters.rotationX;
      object.rotation.y =
        command.parameters.rotationY;
      object.rotation.z =
        command.parameters.rotationZ;
      /**
       * gets the uuid of both robots
       */
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
            if (
              model.uuid === command.parameters.uuid
            ) {
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
       * keycode 27 is esc(to escape the screeching from the robot)
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
       * if toRobot == true, camera moves to robot1.
       * actions come from the server
       * cameracontrols hardcoded atm, will be changed in the future
       */
      if (toRobot1) {
        if (command.parameters.uuid == robot1) {
          if (command.parameters.action == "z-") {
            camera.position.x = object.position.x;
            camera.position.y =
              object.position.y + 2;
            camera.position.z =
              object.position.z + 2;
          } else if (
            command.parameters.action == "z+"
          ) {
            camera.position.x = object.position.x;
            camera.position.y =
              object.position.y + 2;
            camera.position.z =
              object.position.z - 2;
          } else if (
            command.parameters.action == "x-"
          ) {
            camera.position.x =
              object.position.x + 2;
            camera.position.y =
              object.position.y + 2;
            camera.position.z = object.position.z;
          } else if (
            command.parameters.action == "x+"
          ) {
            camera.position.x =
              object.position.x - 2;
            camera.position.y =
              object.position.y + 2;
            camera.position.z = object.position.z;
          }
          //sets the target from orbitcontrol to the robot
          cameraControls.target.set(
            object.position.x,
            object.position.y,
            object.position.z
          );
        }
      }
      if (toRobot2) {
        if (command.parameters.uuid == robot2) {
          if (command.parameters.action == "z-") {
            camera.position.x = object.position.x;
            camera.position.y =
              object.position.y + 2;
            camera.position.z =
              object.position.z + 2;
          } else if (
            command.parameters.action == "z+"
          ) {
            camera.position.x = object.position.x;
            camera.position.y =
              object.position.y + 2;
            camera.position.z =
              object.position.z - 2;
          } else if (
            command.parameters.action == "x-"
          ) {
            camera.position.x =
              object.position.x + 2;
            camera.position.y =
              object.position.y + 2;
            camera.position.z = object.position.z;
          } else if (
            command.parameters.action == "x+"
          ) {
            camera.position.x =
              object.position.x - 2;
            camera.position.y =
              object.position.y + 2;
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
  })


  /**
   * loader for files with the gltf format
   */
  const loader = new THREE.GLTFLoader();

  /**
   * Function to spawn in a single rack model
   * @param {number} x 
   * @param {number} y 
   * @param {number} z 
   */
  function rack(x, y, z) {
    loader.load(
      "models/rackding.gltf",
      function (gltf) {
        const reset = z;
        gltf.scene.position.x = x;
        gltf.scene.position.y = y;
        gltf.scene.position.z = z;
        gltf.scene.scale.set(0.023, 0.023, 0.023);
        scene.add(gltf.scene);
      }
    );
  }

  /**
   * Function to spawn in a single box model
   * @param {number} x 
   * @param {number} y 
   * @param {number} z 
   * @param {any} uuid 
   */
  function box(x, y, z, uuid) {
    loader.load(
      "models/doazespiegelt.gltf",
      function (gltf) {
        const root = gltf.scene;
        gltf.scene.position.x = x;
        gltf.scene.position.y = y;
        gltf.scene.position.z = z;
        gltf.scene.scale.set(1, 1, 1);
        root.uuid = uuid;
        root.scale.y = -1;
        models.push(root.getObjectByName("Scene"));
        scene.add(root);
      }
    );
  }
  /**
   * function to spawn in a single truck model
   * @param {number} x 
   * @param {number} y 
   * @param {number} z 
   * @param {any} uuid 
   */
  function truck(x, y, z, uuid) {
    loader.load(
      "models/libratruck.gltf",
      function (gltf) {
        const root = gltf.scene;
        gltf.scene.position.x = x;
        gltf.scene.position.y = y;
        gltf.scene.position.z = z;
        gltf.scene.scale.set(0.07, 0.07, 0.07);
        root.uuid = uuid;
        models.push(root.getObjectByName("Scene"));
        scene.add(root);
      }
    );
  }

  /**
   * spawns in racks to the scene
   * when the planesizes are changed, the total racks also changes to fit the plane
   * when counter === 3 a big hallway will be created
   * the rack() thats commeted out can be use to spawn in a rack right next to the other rack
   * the robots tend to have problems with collisionboxes that are right next to each other
   * so it's not encouraged to enable this
   */
  function spawn() {
    let z = bottomcornery;
    let counter = 0;

    for (let i = 0; i < zracks; i++) {
      let x = bottomcornerx;
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
  init();
  animate();
};
