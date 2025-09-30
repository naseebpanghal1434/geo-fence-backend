#!/usr/bin/env python3
import socket
import json
from http.server import HTTPServer, BaseHTTPRequestHandler

class TestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/api/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            response = {
                "status": "UP",
                "message": "Test server running - Spring Boot is starting",
                "timestamp": "temp-test-server",
                "server_ip": socket.gethostbyname(socket.gethostname())
            }
            self.wfile.write(json.dumps(response).encode())
        elif self.path == '/swagger-ui.html' or self.path.startswith('/swagger'):
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            html = f"""
            <html>
            <head><title>Spring Boot Starting...</title></head>
            <body>
                <h1>Spring Boot Application is Starting</h1>
                <p>The Spring Boot application is currently downloading dependencies and starting up.</p>
                <p>Server IP: {socket.gethostbyname(socket.gethostname())}</p>
                <p>This is a temporary test server. The real application will be available shortly.</p>
                <p>Check <a href="/api/health">/api/health</a> endpoint</p>
            </body>
            </html>
            """
            self.wfile.write(html.encode())
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b'404 - Not Found')

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', 8080), TestHandler)
    print(f"Test server running on http://0.0.0.0:8080")
    print(f"Try: http://{socket.gethostbyname(socket.gethostname())}:8080/api/health")
    server.serve_forever()