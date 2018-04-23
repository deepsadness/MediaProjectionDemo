var net = require('net');

var HOST = '127.0.0.1'
var PORT = 9000;

net.createServer(function (sock) {
    console.log('CONNETCTED' +
        sock.remoteAddress + ":" + sock.remotePort
    );

    sock.on('data', function (data) {
        console.log("DATA" + sock.remoteAddress
            + ":" + sock.remotePort);
        sock.write('You said "' + data + '"');
    })

    sock.on('close', function (data) {
        console.log('CLOSED' +
            + sock.remoteAddress
            + ":" + sock.remotePort
        );
    })
}).listen(HOST,PORT)

console.log('Server listening on '+HOST+":"+PORT);
