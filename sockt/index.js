var express = require('express')
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);

var ios = require('socket.io-client');

const socket_client = ios('http://localhost:9000');


socket_client.on('connect', function () {
  console.log('connect');

});

socket_client.on('disconnect', function () {
  console.log("disconnect");
});

console.log("Start!!");

io.on('connection', function (socket) {
  socket.on('chat message', function (msg) {
    io.emit('chat message', msg);
  });
  // //将收到的数据发送出去
  socket_client.on('event', function (data) {
    console.log('event come!!');
    io.emit('image', data);
  });
});

app.use(express.static('public'));

app.get('/', function (req, res) {
  res.sendFile(__dirname + '/index.html');
});

http.listen(3000, function () {
  console.log('listening on *:3000');
});