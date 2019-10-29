function parseCommand(input = "") {
    return JSON.parse(input);
}

var socket;

window.onload = function () {
    var camera, scene, renderer;
    var cameraControls;
    var worldObjects = {};

    function init() {
        camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 1, 1000);
        cameraControls = new THREE.OrbitControls(camera);
        camera.position.z = 15;
        camera.position.y = 5;
        camera.position.x = 15;
        cameraControls.update();
        scene = new THREE.Scene();

        renderer = new THREE.WebGLRenderer({ antialias: true });
        renderer.physicallyCorrectLights = true;
        renderer.setPixelRatio(window.devicePixelRatio);
        renderer.setSize(window.innerWidth, window.innerHeight + 5);
        document.body.appendChild(renderer.domElement);
        window.addEventListener('resize', onWindowResize, false);

        var geometry = new THREE.PlaneGeometry(30, 30, 32);
        var texture = new THREE.TextureLoader().load("textures/concrete.jpg");
        texture.wrapS = THREE.RepeatWrapping;
        texture.wrapT = THREE.RepeatWrapping;
        texture.repeat.set(10, 10);
        var material = new THREE.MeshBasicMaterial({ map: texture, side: THREE.DoubleSide });
        var plane = new THREE.Mesh(geometry, material);
        plane.rotation.x = Math.PI / 2.0;
        plane.position.x = 15;
        plane.position.z = 15;
        scene.add(plane);


        //GLTF loader, used to load models and materials from blender
        var loader = new THREE.GLTFLoader();

        //rack needs a x y and z value for a position in the world.
        function rack(x, y, z) {
            loader.load(
                'models/rackding.gltf', //load the model you want to use
                function (gltf) {
                    /*gltf.scene.scale.set(0.023, 0.023, 0.023); //if model is to big or too small, change this value
                    gltf.scene.position.x = x;
                    gltf.scene.position.y = y;
                    gltf.scene.position.z = z;
                    scene.add(gltf.scene);
                    var reset = z;
                    //loops to put boxes on each rack
                    for (let i = 0; i < 5; i++) {
                        for (let i = 0; i < 3; i++) {
                            box(x, y, z + 0.4);
                            z -= 1;
                        }
                        y += 0.9;
                        z = reset;
                    }





                    gltf.animations; // Array<THREE.AnimationClip>
                    gltf.scene; // THREE.Scene
                    gltf.scenes; // Array<THREE.Scene>
                    gltf.cameras; // Array<THREE.Camera>
                    gltf.asset; // Object*/
                },
                function (xhr) {
                    console.log((xhr.loaded / xhr.total * 100) + '% loaded');
                },
                function (error) {
                    console.log('An error happened');
                }
            );
        }
        //box needs an x y and z value, this function is uses in rack, but could also be used for other stuff if needed
        function box(x, y, z) {
            loader.load(
                'models/doos.gltf',
                function (gltf) {
                    gltf.scene.scale.set(1, 1, 1);
                    gltf.scene.position.x = x;
                    gltf.scene.position.y = y;
                    gltf.scene.position.z = z;
                    scene.add(gltf.scene);

                    gltf.animations; // Array<THREE.AnimationClip>
                    gltf.scene; // THREE.Scene
                    gltf.scenes; // Array<THREE.Scene>
                    gltf.cameras; // Array<THREE.Camera>
                    gltf.asset; // Object
                },
                function (xhr) {
                    console.log((xhr.loaded / xhr.total * 100) + '% loaded');
                },
                function (error) {
                    console.log('An error happened');
                }
            );
        }
        //spawns all the (filled) racks on the plane.
        function spawn() {
            var z = 3.5;
            var counter = 0; //counter is used to make a big opening in the middle
            for (let i = 0; i < 6; i++) {

                var x = 4;
                for (let i = 0; i < 5; i++) {

                    rack(x, 0, z);
                    rack(x + 1.5, 0, z);
                    x += 5;
                }
                counter++;
                if (counter == 3) {
                    z += 7;
                }
                else {
                    z += 4
                }
            }
        }

        //audio for the robot
        var listener = new THREE.AudioListener();
        camera.add(listener);
        var sound = new THREE.PositionalAudio(listener);
        var audioLoader = new THREE.AudioLoader();
        var count = 0;
        audioLoader.load('sounds/he.ogg', function (buffer) {

            sound.setBuffer(buffer);
            sound.setRefDistance(100);
            sound.setVolume(1); //don't go over a value of 10, just don't do it
            sound.setLoop(true);
            if (count == 0) {
                sound.play();
                count++;
            }

        });
        spawn();


        //Pointlight, position can/should change. 
        const color = 0XFFFFFF;
        const intensity = 1;
        const light = new THREE.PointLight(color, intensity);
        light.power = 1200;
        light.decay = 1;
        light.distance = Infinity;
        light.position.set(30, 20, 10);
        scene.add(light);
    }

    function onWindowResize() {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    }

    function animate() {
        requestAnimationFrame(animate);
        cameraControls.update();
        renderer.render(scene, camera);
    }

    /*
     * Hier wordt de socketcommunicatie geregeld. Er wordt een nieuwe websocket aangemaakt voor het webadres dat we in
     * de server geconfigureerd hebben als connectiepunt (/connectToSimulation). Op de socket wordt een .onmessage
     * functie geregistreerd waarmee binnenkomende berichten worden afgehandeld.
     */
    //socket = new WebSocket("ws://" + window.location.hostname + ":" + window.location.port + "/connectToSimulation");
    socket = new WebSocket("ws://localhost:8080" + "/connectToSimulation");
    socket.onmessage = function (event) {
        //Hier wordt het commando dat vanuit de server wordt gegeven uit elkaar gehaald
        var command = parseCommand(event.data);

        //Wanneer het commando is "object_update", dan wordt deze code uitgevoerd. Bekijk ook de servercode om dit goed te begrijpen.
        if (command.command == "object_update") {
            //Wanneer het object dat moet worden geupdate nog niet bestaat (komt niet voor in de lijst met worldObjects op de client),
            //dan wordt het 3D model eerst aangemaakt in de 3D wereld.
            if (Object.keys(worldObjects).indexOf(command.parameters.uuid) < 0) {
                //Wanneer het object een robot is, wordt de code hieronder uitgevoerd.
                if (command.parameters.type == "robot") {
                    var geometry = new THREE.BoxGeometry(0.9, 0.3, 0.9);
                    var cubeMaterials = [
                        new THREE.MeshBasicMaterial({ map: new THREE.TextureLoader().load("textures/robot_side.png"), side: THREE.DoubleSide }), //LEFT
                        new THREE.MeshBasicMaterial({ map: new THREE.TextureLoader().load("textures/robot_side.png"), side: THREE.DoubleSide }), //RIGHT
                        new THREE.MeshBasicMaterial({ map: new THREE.TextureLoader().load("textures/robot_top.png"), side: THREE.DoubleSide }), //TOP
                        new THREE.MeshBasicMaterial({ map: new THREE.TextureLoader().load("textures/robot_bottom.png"), side: THREE.DoubleSide }), //BOTTOM
                        new THREE.MeshBasicMaterial({ map: new THREE.TextureLoader().load("textures/robot_front.png"), side: THREE.DoubleSide }), //FRONT
                        new THREE.MeshBasicMaterial({ map: new THREE.TextureLoader().load("textures/robot_front.png"), side: THREE.DoubleSide }), //BACK
                    ];

                    var material = new THREE.MeshFaceMaterial(cubeMaterials);
                    var robot = new THREE.Mesh(geometry, material);
                    robot.position.y = 0.15;
                    //robot.add(sound);

                    var group = new THREE.Group();
                    group.add(robot);
                    //group.add(rack);
                    //group.add(box);

                    scene.add(group);
                    worldObjects[command.parameters.uuid] = group;
                }
                else if (command.parameters.type == "rack") {
                    var geometry = new THREE.BoxGeometry(4, 2, 8);
                    var material = new THREE.MeshBasicMaterial({ color: 0x00ff00 });
                    var group = new THREE.Group();

                    var rack = new THREE.Mesh(geometry, material);
                    group.add(rack);

                    scene.add(group);
                    worldObjects[command.parameters.uuid] = group;

                    var testGeometry = new THREE.BoxGeometry(0.2, 2, 0.2);
                    var testMaterial = new THREE.MeshBasicMaterial({ color: 0x340000 });
                    var test = new THREE.Mesh(testGeometry, testMaterial);
                    test.position.x = 7.0;
                    test.position.y = 0;
                    test.position.z = 10;
                    scene.add(test);

                    var testGeometry = new THREE.BoxGeometry(0.2, 2, 0.2);
                    var testMaterial = new THREE.MeshBasicMaterial({ color: 0x340000 });
                    var test = new THREE.Mesh(testGeometry, testMaterial);
                    test.position.x = 13.0;
                    test.position.y = 0;
                    test.position.z = 10;
                    scene.add(test);
                }
            }

            /*
             * Deze code wordt elke update uitgevoerd. Het update alle positiegegevens van het 3D object.
             */
            var object = worldObjects[command.parameters.uuid];

            object.position.x = command.parameters.x;
            object.position.y = command.parameters.y;
            object.position.z = command.parameters.z;

            object.rotation.x = command.parameters.rotationX;
            object.rotation.y = command.parameters.rotationY;
            object.rotation.z = command.parameters.rotationZ;
        }
    }

    init();
    animate();
}