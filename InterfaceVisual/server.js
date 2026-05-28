const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const net = require('net');
const dgram = require('dgram');

const PORT = 3000;
const WORKSPACE_DIR = path.resolve(__dirname, '..');

let activeJavaProcess = null;
let sseClients = [];
let clientSockets = {}; // sessionId -> net.Socket
let multicastSocket = null;

const SOCKETS_SRC_DIR = path.resolve(WORKSPACE_DIR, 'EntregaSockets', 'src');

function scanSocketExercises() {
    const list = {};
    if (!fs.existsSync(SOCKETS_SRC_DIR)) return list;

    const folders = fs.readdirSync(SOCKETS_SRC_DIR);
    folders.forEach(folder => {
        const folderPath = path.join(SOCKETS_SRC_DIR, folder);
        if (!fs.statSync(folderPath).isDirectory()) return;

        // Find java files
        const files = fs.readdirSync(folderPath).filter(f => f.endsWith('.java'));
        let serverClass = '';

        // Try to identify the server class (containing main)
        for (const file of files) {
            const filePath = path.join(folderPath, file);
            const content = fs.readFileSync(filePath, 'utf8');
            if (content.includes('public static void main')) {
                if (content.includes('ServerSocket') || content.includes('MulticastSocket') || file.startsWith('Servidor') || file.startsWith('SistemaCentral') || file.startsWith('Chat')) {
                    serverClass = `${folder}.${file.replace('.java', '')}`;
                    break;
                }
            }
        }

        if (!serverClass && files.length > 0) {
            const mainFile = files.find(f => fs.readFileSync(path.join(folderPath, f), 'utf8').includes('public static void main'));
            if (mainFile) {
                serverClass = `${folder}.${mainFile.replace('.java', '')}`;
            }
        }

        if (!serverClass) return; // Skip if no runnable class

        let name = folder.charAt(0).toUpperCase() + folder.slice(1);
        let desc = 'Exercício de Socket customizado.';
        let port = 0;

        if (folder === 'fortune') {
            name = 'Servidor de Fortunes';
            desc = 'Biscoito da sorte chinês com operações GET, ADD, UPD e LST.';
            port = 12345;
        } else if (folder === 'inteiros') {
            name = 'Processamento de Inteiros';
            desc = 'Recebe uma sequência de inteiros e calcula a SOMA ou MULT após o EOF.';
            port = 12346;
        } else if (folder === 'forca') {
            name = 'Jogo da Forca Remoto';
            desc = 'Jogo de adivinhação de palavras remoto para um jogador.';
            port = 12347;
        } else if (folder === 'banco') {
            name = 'Instituição Financeira';
            desc = 'Simulação de transações bancárias concorrentes de múltiplos clientes.';
            port = 12348;
        } else if (folder === 'lojas') {
            name = 'Rede de Lojas de Departamento';
            desc = 'Centralização de vendas diárias recebendo dados de filiais simultâneas.';
            port = 12349;
        } else if (folder === 'arquivos') {
            name = 'Servidor de Arquivos';
            desc = 'Serviço de upload e download de arquivos no servidor.';
            port = 8350;
        } else if (folder === 'chat') {
            name = 'Chat Multicast';
            desc = 'Chat em grupo em tempo real usando comunicação via Multicast UDP.';
            port = 12351;
        }

        list[`socket_${folder}`] = {
            id: `socket_${folder}`,
            name: name,
            class: serverClass,
            dir: 'EntregaSockets',
            description: desc,
            isSocket: true,
            port: port,
            folder: folder
        };
    });

    return list;
}

function getExerciseById(id) {
    if (id && id.startsWith('socket_')) {
        const socketExercises = scanSocketExercises();
        return socketExercises[id];
    }
    return EXERCISES[id];
}

// Java binary encoding utilities for ServidorArquivos
function writeJavaUTF(socket, str) {
    const buf = Buffer.from(str, 'utf8');
    const lenBuf = Buffer.alloc(2);
    lenBuf.writeUInt16BE(buf.length, 0);
    socket.write(Buffer.concat([lenBuf, buf]));
}

function writeJavaLong(socket, val) {
    const buf = Buffer.alloc(8);
    buf.writeBigInt64BE(BigInt(val), 0);
    socket.write(buf);
}

function readJavaUTF(dataBuffer) {
    if (dataBuffer.length < 2) return null;
    const len = dataBuffer.readUInt16BE(0);
    if (dataBuffer.length < 2 + len) return null;
    return dataBuffer.toString('utf8', 2, 2 + len);
}

const EXERCISES = {
    bar: {
        id: 'bar',
        name: 'Atendimento no Bar',
        class: 'bar.BarPrincipal',
        dir: 'EntregaThreads',
        description: 'Simula a concorrência entre Clientes, Garçons e Bartender usando Monitores.'
    },
    barbearia: {
        id: 'barbearia',
        name: 'Barbeiro Dorminhoco',
        class: 'barbearia.SistemaBarbearia',
        dir: 'EntregaThreads',
        description: 'Implementação do Barbeiro Dorminhoco usando cadeiras de espera e Monitores.'
    },
    filosofos: {
        id: 'filosofos',
        name: 'O Jantar dos Filósofos',
        class: 'filosofos.JantarFilosofos',
        dir: 'EntregaThreads',
        description: 'Simula os filósofos que alternam entre meditar e comer, evitando deadlock.'
    },
    roletas: {
        id: 'roletas',
        name: 'Problema das Roletas',
        class: 'classicos.ProblemaRoletas',
        dir: 'EntregaThreads',
        description: 'Múltiplas roletas atualizam concorrentemente um contador compartilhado.'
    },
    contas: {
        id: 'contas',
        name: 'Contas Bancárias',
        class: 'classicos.ContasBancarias',
        dir: 'EntregaThreads',
        description: 'Simula depósitos, saques, transferências e juros concorrentes em contas.'
    },
    prodcons_monitor: {
        id: 'prodcons_monitor',
        name: 'Produtor/Consumidor (Monitores)',
        class: 'classicos.ProdutorConsumidorMonitor',
        dir: 'EntregaThreads',
        description: 'Produtor e Consumidor compartilhando um buffer limitado usando Monitores.'
    },
    prodcons_semaforo: {
        id: 'prodcons_semaforo',
        name: 'Produtor/Consumidor (Semáforos)',
        class: 'classicos.ProdutorConsumidorSemaforo',
        dir: 'EntregaThreads',
        description: 'Produtor e Consumidor compartilhando um buffer limitado usando Semáforos.'
    }
};

function findJavaFiles(dir, fileList = []) {
    if (!fs.existsSync(dir)) return fileList;
    const files = fs.readdirSync(dir);
    files.forEach(file => {
        const filePath = path.join(dir, file);
        if (fs.statSync(filePath).isDirectory()) {
            findJavaFiles(filePath, fileList);
        } else if (file.endsWith('.java')) {
            fileList.push(filePath);
        }
    });
    return fileList;
}

function cleanActiveResources() {
    if (activeJavaProcess) {
        try {
            if (process.platform === 'win32') {
                spawn('taskkill', ['/pid', activeJavaProcess.pid, '/f', '/t']);
            } else {
                activeJavaProcess.kill('SIGINT');
            }
        } catch(e){}
        activeJavaProcess = null;
    }
    // Clean up active client sockets
    for (const sid in clientSockets) {
        try {
            clientSockets[sid].destroy();
        } catch(e){}
    }
    clientSockets = {};

    // Clean up multicast socket
    if (multicastSocket) {
        try {
            multicastSocket.close();
        } catch(e){}
        multicastSocket = null;
    }
}

function broadcast(event, data) {
    const message = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
    sseClients.forEach(client => {
        client.write(message);
    });
}

const server = http.createServer((req, res) => {
    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    const pathname = parsedUrl.pathname;

    // --- Servir Arquivos Estáticos ---
    if (pathname === '/' || pathname === '/index.html') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(fs.readFileSync(path.join(__dirname, 'index.html')));
        return;
    }
    if (pathname === '/style.css') {
        res.writeHead(200, { 'Content-Type': 'text/css' });
        res.end(fs.readFileSync(path.join(__dirname, 'style.css')));
        return;
    }
    if (pathname === '/app.js') {
        res.writeHead(200, { 'Content-Type': 'application/javascript; charset=utf-8' });
        res.end(fs.readFileSync(path.join(__dirname, 'app.js')));
        return;
    }

    // --- API REST ---
    if (pathname === '/api/exercises' && req.method === 'GET') {
        const allExercises = [
            ...Object.values(EXERCISES),
            ...Object.values(scanSocketExercises())
        ];
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(allExercises));
        return;
    }

    if (pathname === '/api/stop' && req.method === 'POST') {
        cleanActiveResources();
        broadcast('status', { msg: 'Processo encerrado pelo usuário.', type: 'info' });
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));
        return;
    }

    if (pathname === '/api/start' && req.method === 'POST') {
        const id = parsedUrl.searchParams.get('id');
        const exercise = getExerciseById(id);

        if (!exercise) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Exercício não encontrado' }));
            return;
        }

        cleanActiveResources();

        const exercisePath = path.join(WORKSPACE_DIR, exercise.dir);
        broadcast('status', { msg: `Compilando arquivos Java para ${exercise.name}...`, type: 'compile' });

        const javaFiles = findJavaFiles(path.join(exercisePath, 'src'));
        if (javaFiles.length === 0) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Nenhum arquivo Java encontrado para compilar.' }));
            return;
        }

        const binPath = path.join(exercisePath, 'bin');
        if (!fs.existsSync(binPath)) {
            fs.mkdirSync(binPath, { recursive: true });
        }

        const javac = spawn('javac', ['-d', 'bin', ...javaFiles], { cwd: exercisePath });
        
        let compileErrors = '';
        javac.stderr.on('data', (data) => {
            compileErrors += data.toString();
        });

        javac.on('close', (code) => {
            if (code !== 0) {
                broadcast('status', { msg: `Erro de compilação! Código: ${code}`, type: 'error' });
                broadcast('stdout', { text: compileErrors, isError: true });
                return;
            }

            broadcast('status', { msg: `Compilação concluída com sucesso! Iniciando Java...`, type: 'run' });

            activeJavaProcess = spawn('java', ['-cp', 'bin', exercise.class], { cwd: exercisePath });

            activeJavaProcess.stdout.on('data', (data) => {
                broadcast('stdout', { text: data.toString() });
            });

            activeJavaProcess.stderr.on('data', (data) => {
                broadcast('stdout', { text: data.toString(), isError: true });
            });

            activeJavaProcess.on('close', (exitCode) => {
                broadcast('status', { msg: `Programa finalizado com código de saída ${exitCode}.`, type: 'info' });
                activeJavaProcess = null;
            });
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, msg: 'Compilação iniciada.' }));
        return;
    }

    // --- PROXIES DE SOCKETS ---
    if (pathname === '/api/socket/connect' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { sessionId, port } = JSON.parse(body);
                if (!sessionId || !port) {
                    res.writeHead(400, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Faltando sessionId ou port' }));
                    return;
                }

                if (clientSockets[sessionId]) {
                    try { clientSockets[sessionId].destroy(); } catch(e){}
                }

                const socket = new net.Socket();
                clientSockets[sessionId] = socket;

                socket.connect(port, '127.0.0.1', () => {
                    broadcast('socket_connected', { sessionId });
                });

                socket.on('data', data => {
                    broadcast('socket_data', { sessionId, data: data.toString('utf8') });
                });

                socket.on('close', () => {
                    broadcast('socket_closed', { sessionId });
                    delete clientSockets[sessionId];
                });

                socket.on('error', err => {
                    broadcast('socket_error', { sessionId, error: err.message });
                });

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/send' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { sessionId, data, closeWrite } = JSON.parse(body);
                const socket = clientSockets[sessionId];
                if (!socket) {
                    res.writeHead(404, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Conexão não encontrada' }));
                    return;
                }

                if (data !== undefined) {
                    socket.write(data);
                }

                if (closeWrite) {
                    socket.end();
                }

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/disconnect' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { sessionId } = JSON.parse(body);
                const socket = clientSockets[sessionId];
                if (socket) {
                    socket.destroy();
                    delete clientSockets[sessionId];
                }
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/run-client' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { clientClass, args } = JSON.parse(body);
                if (!clientClass) {
                    res.writeHead(400, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Faltando clientClass' }));
                    return;
                }

                const exercisePath = path.join(WORKSPACE_DIR, 'EntregaSockets');
                const javaFiles = findJavaFiles(path.join(exercisePath, 'src'));
                const javac = spawn('javac', ['-d', 'bin', ...javaFiles], { cwd: exercisePath });
                
                javac.on('close', (code) => {
                    if (code !== 0) {
                        broadcast('stdout', { text: `[Erro] Falha ao compilar cliente ${clientClass}`, isError: true });
                        return;
                    }

                    broadcast('stdout', { text: `[Cliente] Iniciando ${clientClass} ${args ? args.join(' ') : ''}...` });
                    const clientProc = spawn('java', ['-cp', 'bin', clientClass, ...(args || [])], { cwd: exercisePath });

                    clientProc.stdout.on('data', (data) => {
                        broadcast('stdout', { text: `[${clientClass}] ${data.toString()}` });
                    });

                    clientProc.stderr.on('data', (data) => {
                        broadcast('stdout', { text: `[${clientClass}] ${data.toString()}`, isError: true });
                    });

                    clientProc.on('close', (exitCode) => {
                        broadcast('stdout', { text: `[${clientClass}] Finalizado com código ${exitCode}` });
                    });
                });

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/file-upload' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { filename, content } = JSON.parse(body);
                if (!filename || content === undefined) {
                    res.writeHead(400, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Faltando filename ou content' }));
                    return;
                }

                const socket = new net.Socket();
                socket.connect(8350, '127.0.0.1', () => {
                    writeJavaUTF(socket, "UPLOAD");
                    writeJavaUTF(socket, filename);
                    const fileBuf = Buffer.from(content, 'utf8');
                    writeJavaLong(socket, fileBuf.length);
                    socket.write(fileBuf);
                });

                let responseData = Buffer.alloc(0);
                socket.on('data', (chunk) => {
                    responseData = Buffer.concat([responseData, chunk]);
                });

                socket.on('end', () => {
                    const msg = readJavaUTF(responseData);
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ success: true, message: msg || 'Upload concluído' }));
                });

                socket.on('error', (err) => {
                    res.writeHead(500, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: err.message }));
                });
            } catch(e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/file-download' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { filename } = JSON.parse(body);
                if (!filename) {
                    res.writeHead(400, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Faltando filename' }));
                    return;
                }

                const socket = new net.Socket();
                socket.connect(8350, '127.0.0.1', () => {
                    writeJavaUTF(socket, "DOWNLOAD");
                    writeJavaUTF(socket, filename);
                });

                let responseData = Buffer.alloc(0);
                socket.on('data', (chunk) => {
                    responseData = Buffer.concat([responseData, chunk]);
                });

                socket.on('end', () => {
                    if (responseData.length < 8) {
                        res.writeHead(500, { 'Content-Type': 'application/json' });
                        res.end(JSON.stringify({ error: 'Resposta inválida do servidor de arquivos' }));
                        return;
                    }
                    const length = responseData.readBigInt64BE(0);
                    if (length === -1n) {
                        res.writeHead(404, { 'Content-Type': 'application/json' });
                        res.end(JSON.stringify({ error: 'Arquivo não encontrado' }));
                    } else {
                        const fileContent = responseData.toString('utf8', 8);
                        res.writeHead(200, { 'Content-Type': 'application/json' });
                        res.end(JSON.stringify({ success: true, content: fileContent }));
                    }
                });

                socket.on('error', (err) => {
                    res.writeHead(500, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: err.message }));
                });
            } catch(e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/chat-join' && req.method === 'POST') {
        try {
            if (!multicastSocket) {
                multicastSocket = dgram.createSocket({ type: 'udp4', reuseAddr: true });
                
                multicastSocket.on('message', (msg) => {
                    broadcast('chat_message', { text: msg.toString('utf8') });
                });

                multicastSocket.bind(12351, () => {
                    try {
                        multicastSocket.addMembership('224.0.0.1');
                    } catch(e){}
                });
            }
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true }));
        } catch (e) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: e.message }));
        }
        return;
    }

    if (pathname === '/api/socket/chat-send' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { nickname, message } = JSON.parse(body);
                const formatada = `${nickname}: ${message}`;
                const bytes = Buffer.from(formatada, 'utf8');

                const client = dgram.createSocket('udp4');
                client.send(bytes, 0, bytes.length, 12351, '224.0.0.1', (err) => {
                    client.close();
                });

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (pathname === '/api/socket/chat-leave' && req.method === 'POST') {
        try {
            if (multicastSocket) {
                try { multicastSocket.dropMembership('224.0.0.1'); } catch(e){}
                multicastSocket.close();
                multicastSocket = null;
            }
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true }));
        } catch (e) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: e.message }));
        }
        return;
    }

    // --- Stream SSE (Server-Sent Events) ---
    if (pathname === '/api/stream') {
        res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
        });

        sseClients.push(res);

        req.on('close', () => {
            sseClients = sseClients.filter(client => client !== res);
        });
        return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Rota não encontrada');
});

process.on('SIGINT', () => {
    cleanActiveResources();
    process.exit();
});
process.on('exit', () => {
    cleanActiveResources();
});

server.listen(PORT, () => {
    console.log(`[Interface Visual] Servidor rodando em http://localhost:${PORT}`);
});
