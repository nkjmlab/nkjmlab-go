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

