const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const PORT = 3000;
const WORKSPACE_DIR = path.resolve(__dirname, '..');

let activeJavaProcess = null;
let sseClients = [];

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
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(Object.values(EXERCISES)));
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
        const exercise = EXERCISES[id];

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
