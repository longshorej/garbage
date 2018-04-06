# unix-domain-socket-close

A server and client program. The client connects to the server over Unix Domain Sockets, writes a string, and closes the write side of its connection. The server then writes some data after it detects end of input. The client should then print this data to the console.

