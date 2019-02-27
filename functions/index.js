const functions = require('firebase-functions');

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });
const request = require('request-promise');
const admin = require('firebase-admin');
admin.initializeApp();

//this is a real time database trigger onWrite. There are many triggers refer to documentation
//event is a data snapshot
exports.indexPostsToElastic = functions.database.ref('/posts/{post_id}')
	.onWrite((change,context) => {
		
		if (change.before.exists()) {
			console.log("Only edit data when it is first created.");
		}
		if (!change.after.exists()) {
			console.log("Exit when the data is deleted.");
		}
		
		let postData = change.after.val();
		console.log('Uppercasing', context.params.post_id);
		let post_id = context.params.post_id;
		
		console.log('Indexing post:',postData);
		
		let elasticSearchConfig = functions.config().elasticsearch;
		let elasticSearchUrl = elasticSearchConfig.url + 'posts/post/' + post_id;
		let elasticSearchMethod = postData ? 'POST' : 'DELETE';
		
		let elasticSearchRequest = request({
			url: elasticSearchUrl,
			method: elasticSearchMethod,
			auth:{
				username: elasticSearchConfig.username,
				password: elasticSearchConfig.password,
			},
			body: postData,
			json : true
		 });
		 return request(elasticSearchRequest).then(response => {
			console.log("Elastic search response", response);
			return response;
		 });
	});
	
