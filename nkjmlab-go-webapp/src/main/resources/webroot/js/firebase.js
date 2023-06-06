var firebaseConfig = {
  apiKey: "AIzaSyDa5VSgLjaFmPpeitARYH_ArmlOsQuoXOQ",
  authDomain: "toho-go-fb.firebaseapp.com",
  databaseURL: "https://toho-go-fb.firebaseio.com",
  projectId: "toho-go-fb",
  storageBucket: "toho-go-fb.appspot.com",
  messagingSenderId: "970527770922",
  appId: "1:970527770922:web:370dc1c9bf2e3d4ab63653",
  measurementId: "G-VBSF7TWVQX"
};
firebase.initializeApp(firebaseConfig);

function firebaseSignOut(callback) {
  firebase.auth().signOut().then(function (e) {
    const req = new JsonRpcRequest(
      getAuthRpcServiceUrl(), "signoutFromFirebase", [],
      function (data) {
        console.log("signout");
        if (callback) {
          callback();
        }
      });
    const client = new JsonRpcClient(req);
    client.rpc();
  }).catch(function (error) {
    console.log(error);
  });
}

