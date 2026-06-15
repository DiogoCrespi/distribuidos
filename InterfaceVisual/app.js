document.addEventListener('DOMContentLoaded', () => {
    // --- ESTADO GLOBAL ---
    let currentExerciseId = 'bar';
    let sseSource = null;
    let isRunning = false;
    let bufferItems = [];

    // --- SOCKET STATE VARIABLES ---
    let currentSocketSessionId = null;
    let forcaSessionId = null;
    let bankSessionId = null;
    let bankAccountNum = null;
    let chatSessionId = null;
    let activeChatNickname = '';
    let integersList = [];
    let uploadedFiles = [];
    let forcaErros = 0;
    let fortuneTimeout = null;

    // --- FILÓSOFOS STATE ---
    const philosopherStates = {
        0: 'meditating',
        1: 'meditating',
        2: 'meditating',
        3: 'meditating',
        4: 'meditating'
    };

    // --- ELEMENTOS DOM ---
    const menuButtons = document.querySelectorAll('.menu-btn');
    const currentTitle = document.getElementById('current-title');
    const currentDesc = document.getElementById('current-desc');
    const statusBadge = document.getElementById('status-badge');
    const btnStart = document.getElementById('btn-start');
    const btnStop = document.getElementById('btn-stop');
    const consoleOutput = document.getElementById('console-output');
    const btnClearConsole = document.getElementById('btn-clear-console');

    // Elementos do Bar
    const barClientsContainer = document.getElementById('bar-clients');
    const barWaitersContainer = document.getElementById('bar-waiters');
    const barOrdersQueue = document.getElementById('bar-orders-queue');
    const barRodadaText = document.getElementById('bar-rodada');
    const barCapacidadeText = document.getElementById('bar-capacidade');

    // Elementos da Barbearia
    const barbeiroActor = document.getElementById('barbeiro-actor');
    const chairClient = document.getElementById('chair-client');
    const waitingChairs = document.querySelectorAll('#waiting-chairs .chair-spot-visual');
    const barberClientsList = document.getElementById('barber-clients-list');

    // Elementos dos Filósofos
    const philCards = document.querySelectorAll('.phil-card');
    const hashiItems = document.querySelectorAll('.hashi-item');

    // Elementos das Roletas
    const roletasContador = document.getElementById('roletas-contador');
    const roletasEsperado = document.getElementById('roletas-esperado');
    const roletaNodes = document.querySelectorAll('.roleta-node');

    // Elementos das Contas Bancárias
    const balanceA = document.getElementById('balance-a');
    const balanceB = document.getElementById('balance-b');
    const transactionsList = document.getElementById('transactions-list');

    // Elementos do Produtor/Consumidor
    const prodconsProducer = document.getElementById('prodcons-producer');
    const prodconsConsumer = document.getElementById('prodcons-consumer');
    const bufferSlots = document.querySelectorAll('.buffer-slot');

    function repositionPhilosophersRing() {
        const ring = document.getElementById('philosophers-ring');
        if (!ring) return;
        
        const philNodes = ring.querySelectorAll('.phil-circle-node');
        const hashiNodes = ring.querySelectorAll('.hashi-stick');
        const N = philNodes.length;
        if (N === 0) return;
        
        const width = ring.clientWidth || 400;
        const height = ring.clientHeight || 400;
        const cx = width / 2;
        const cy = height / 2;
        
        // Posições circulares
        const philRadius = Math.min(cx, cy) - 46;
        const hashiRadius = philRadius - 36;
        
        philNodes.forEach((node) => {
            const id = parseInt(node.getAttribute('data-id'));
            const theta = (2 * Math.PI * id) / N - Math.PI / 2;
            const x = cx + Math.cos(theta) * philRadius;
            const y = cy + Math.sin(theta) * philRadius;
            node.style.left = `${x}px`;
            node.style.top = `${y}px`;
        });
        
        hashiNodes.forEach((node) => {
            const id = parseInt(node.getAttribute('data-id'));
            const theta = (2 * Math.PI * (id - 0.5)) / N - Math.PI / 2;
            const x = cx + Math.cos(theta) * hashiRadius;
            const y = cy + Math.sin(theta) * hashiRadius;
            
            node.style.left = `${x}px`;
            node.style.top = `${y}px`;
            const angleDeg = (theta * 180) / Math.PI + 90;
            node.style.transform = `translate(-50%, -50%) rotate(${angleDeg}deg)`;
        });
    }
    window.addEventListener('resize', repositionPhilosophersRing);

    // Dicionário de Exercícios para a Interface
    const EXERCISES = {
        bar: {
            title: 'Atendimento no Bar',
            desc: 'Simula a concorrência entre Clientes, Garçons e Bartender usando Monitores.',
            panelId: 'panel-bar'
        },
        barbearia: {
            title: 'Barbeiro Dorminhoco',
            desc: 'Implementação clássica do Barbeiro Dorminhoco usando cadeiras de espera e Monitores.',
            panelId: 'panel-barbearia'
        },
        filosofos: {
            title: 'O Jantar dos Filósofos',
            desc: 'Simula os filósofos alternando entre meditar e comer, usando hashis compartilhados.',
            panelId: 'panel-filosofos'
        },
        roletas: {
            title: 'Problema das Roletas',
            desc: 'Múltiplas roletas concorrentes incrementando um contador global compartilhado.',
            panelId: 'panel-roletas'
        },
        contas: {
            title: 'Contas Bancárias',
            desc: 'Simula depósitos, saques, transferências e crédito de juros concorrentes em duas contas.',
            panelId: 'panel-contas'
        },
        prodcons: {
            title: 'Produtor / Consumidor',
            desc: 'Problema clássico do produtor/consumidor com buffer compartilhado de 5 posições.',
            panelId: 'panel-prodcons'
        },
        rmi_whatsut: {
            title: 'WhatsUT (Chat RMI)',
            desc: 'Sistema de chat distribuído com interface gráfica utilizando Java RMI e o padrão Callback.',
            panelId: 'panel-rmi',
            isSocket: false
        }
    };

    // --- CONFIGURAÇÃO INICIAL ---
    setupSSE();
    resetAllUIs();

    // --- NAVEGAÇÃO ENTRE ABAS ---
    function selectExercise(id, btnElement) {
        if (id === currentExerciseId) return;

        if (isRunning) {
            stopCurrentExercise();
        }

        const activeBtn = document.querySelector('.menu-btn.active');
        if (activeBtn) activeBtn.classList.remove('active');
        btnElement.classList.add('active');

        const activePanel = document.querySelector('.sim-panel.active');
        if (activePanel) activePanel.classList.remove('active');
        
        const targetPanelId = EXERCISES[id].panelId;
        const targetPanel = document.getElementById(targetPanelId);
        if (targetPanel) {
            targetPanel.classList.add('active');
        }

        currentExerciseId = id;
        currentTitle.textContent = EXERCISES[id].title;
        currentDesc.textContent = EXERCISES[id].desc;

        resetAllUIs();
        
        if (EXERCISES[id].isSocket) {
            renderSocketClientUI(id);
        }

        appendLog(`[Interface] Alternado para ${EXERCISES[id].title}`, 'system');
    }

    function initMenuNavigation() {
        const originalButtons = document.querySelectorAll('.nav-menu .menu-btn');
        originalButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                selectExercise(btn.getAttribute('data-id'), btn);
            });
        });

        fetch('/api/exercises')
            .then(res => res.json())
            .then(exercises => {
                const listContainer = document.getElementById('sockets-menu-list');
                if (!listContainer) return;
                listContainer.innerHTML = '';

                exercises.forEach(ex => {
                    if (ex.isSocket) {
                        EXERCISES[ex.id] = {
                            title: ex.name,
                            desc: ex.description,
                            panelId: 'panel-sockets',
                            isSocket: true,
                            port: ex.port,
                            folder: ex.folder,
                            class: ex.class
                        };

                        const btn = document.createElement('button');
                        btn.className = 'menu-btn';
                        btn.setAttribute('data-id', ex.id);
                        btn.innerHTML = `<span class="btn-label">${ex.name}</span>`;
                        btn.addEventListener('click', () => {
                            selectExercise(ex.id, btn);
                        });
                        listContainer.appendChild(btn);
                    }
                });
            })
            .catch(err => console.error('Error fetching exercises:', err));
    }

    initMenuNavigation();

    // Toggle menu de threads
    const categoryThreads = document.getElementById('category-threads');
    const threadsMenuList = document.getElementById('threads-menu-list');
    if (categoryThreads && threadsMenuList) {
        categoryThreads.addEventListener('click', () => {
            const isCollapsed = categoryThreads.classList.toggle('collapsed');
            if (isCollapsed) {
                threadsMenuList.classList.add('collapsed');
            } else {
                threadsMenuList.classList.remove('collapsed');
            }
        });
    }

    // Toggle menu de rmi
    const categoryRmi = document.getElementById('category-rmi');
    const rmiMenuList = document.getElementById('rmi-menu-list');
    if (categoryRmi && rmiMenuList) {
        categoryRmi.addEventListener('click', () => {
            const isCollapsed = categoryRmi.classList.toggle('collapsed');
            if (isCollapsed) {
                rmiMenuList.classList.add('collapsed');
            } else {
                rmiMenuList.classList.remove('collapsed');
            }
        });
    }

    // Toggle menu de sockets
    const categorySockets = document.getElementById('category-sockets');
    const socketsMenuList = document.getElementById('sockets-menu-list');
    if (categorySockets && socketsMenuList) {
        categorySockets.addEventListener('click', () => {
            const isCollapsed = categorySockets.classList.toggle('collapsed');
            if (isCollapsed) {
                socketsMenuList.classList.add('collapsed');
            } else {
                socketsMenuList.classList.remove('collapsed');
            }
        });
    }

    // --- CONTROLE DE PROCESSOS (INICIAR/PARAR) ---
    btnStart.addEventListener('click', async () => {
        btnStart.disabled = true;
        updateBadge('compiling', 'Compilando...');
        
        let startId = currentExerciseId;
        // Caso especial para Produtor / Consumidor: seleciona entre Monitor e Semáforo
        if (currentExerciseId === 'prodcons') {
            const mode = document.querySelector('input[name="prodcons-mode"]:checked').value;
            startId = mode === 'monitor' ? 'prodcons_monitor' : 'prodcons_semaforo';
        }

        resetAllUIs();
        if (EXERCISES[currentExerciseId] && EXERCISES[currentExerciseId].isSocket) {
            renderSocketClientUI(currentExerciseId);
        }

        try {
            const response = await fetch(`/api/start?id=${startId}`, { method: 'POST' });
            const data = await response.json();

            if (data.success) {
                isRunning = true;
                btnStop.disabled = false;
                updateBadge('running', 'Rodando');
                appendLog(`[Interface] Compilação e inicialização disparadas para ${EXERCISES[currentExerciseId].title}...`, 'system');
            } else {
                updateBadge('idle', 'Erro');
                btnStart.disabled = false;
                appendLog(`[Erro] Falha ao iniciar: ${data.error}`, 'stderr');
            }
        } catch (err) {
            updateBadge('idle', 'Erro');
            btnStart.disabled = false;
            appendLog(`[Erro] Erro na requisição: ${err.message}`, 'stderr');
        }
    });

    btnStop.addEventListener('click', () => {
        stopCurrentExercise();
    });

    async function stopCurrentExercise() {
        btnStop.disabled = true;
        try {
            await fetch('/api/stop', { method: 'POST' });
            isRunning = false;
            btnStart.disabled = false;
            updateBadge('idle', 'Ocioso');
            appendLog('[Interface] Execução parada pelo usuário.', 'system');
        } catch (err) {
            appendLog(`[Erro] Erro ao parar: ${err.message}`, 'stderr');
        }
    }

    // --- EVENTOS EM TEMPO REAL (SSE) ---
    function setupSSE() {
        sseSource = new EventSource('/api/stream');

        sseSource.addEventListener('status', (e) => {
            const data = JSON.parse(e.data);
            appendLog(`[Status] ${data.msg}`, 'system');
            
            if (data.type === 'error') {
                updateBadge('idle', 'Erro');
                btnStart.disabled = false;
                btnStop.disabled = true;
                isRunning = false;
            }
        });

        sseSource.addEventListener('stdout', (e) => {
            const data = JSON.parse(e.data);
            appendLog(data.text, data.isError ? 'stderr' : '');

            // Encaminha a saída para o parser do exercício atual
            if (!data.isError) {
                parseLog(currentExerciseId, data.text);
            }
        });

        sseSource.addEventListener('socket_connected', (e) => {
            const data = JSON.parse(e.data);
            appendLog(`[Proxy] Socket cliente conectado: Session ${data.sessionId}`, 'system');
        });

        sseSource.addEventListener('socket_data', (e) => {
            const data = JSON.parse(e.data);
            const { sessionId, data: text } = data;
            
            if (sessionId.startsWith('fortune_')) {
                if (typeof handleFortuneData === 'function') handleFortuneData(text);
            } else if (sessionId.startsWith('inteiros_')) {
                if (typeof handleInteirosData === 'function') handleInteirosData(text);
            } else if (sessionId.startsWith('forca_')) {
                if (typeof handleForcaData === 'function') handleForcaData(text);
            } else if (sessionId.startsWith('banco_')) {
                if (typeof handleBancoData === 'function') handleBancoData(text);
            }
        });

        sseSource.addEventListener('socket_closed', (e) => {
            const data = JSON.parse(e.data);
            appendLog(`[Proxy] Socket cliente fechado: Session ${data.sessionId}`, 'system');
            
            if (data.sessionId.startsWith('forca_')) {
                if (typeof handleForcaClose === 'function') handleForcaClose();
            } else if (data.sessionId.startsWith('banco_')) {
                if (typeof handleBancoClose === 'function') handleBancoClose();
            }
        });

        sseSource.addEventListener('socket_error', (e) => {
            const data = JSON.parse(e.data);
            appendLog(`[Proxy Erro] Session ${data.sessionId}: ${data.error}`, 'stderr');
        });

        sseSource.addEventListener('chat_message', (e) => {
            const data = JSON.parse(e.data);
            if (typeof handleChatMessage === 'function') {
                handleChatMessage(data.text);
            }
        });

        sseSource.onerror = (err) => {
            console.error('SSE connection error:', err);
            document.getElementById('server-status').textContent = 'OFFLINE';
            document.getElementById('server-status').className = 'disconnected-text';
        };
    }

    // --- DIRECIOMANTE DE PARSE DE LOG ---
    function parseLog(exerciseId, text) {
        const lines = text.split('\n');
        lines.forEach(line => {
            line = line.trim();
            if (!line) return;

            switch (exerciseId) {
                case 'bar':
                    parseBarLine(line);
                    break;
                case 'barbearia':
                    parseBarbeariaLine(line);
                    break;
                case 'filosofos':
                    parseFilosofosLine(line);
                    break;
                case 'roletas':
                    parseRoletasLine(line);
                    break;
                case 'contas':
                    parseContasLine(line);
                    break;
                case 'prodcons':
                    parseProdConsLine(line);
                    break;
                case 'socket_lojas':
                    parseLojasLine(line);
                    break;
            }
        });
    }

    // ==========================================
    // 1. PARSER: ATENDIMENTO NO BAR
    // ==========================================
    function parseBarLine(line) {
        // Setup inicial
        if (line.includes('Clientes:')) {
            const match = line.match(/Clientes:\s*(\d+)\s*\|\s*Garcons:\s*(\d+)\s*\|\s*Capacidade:\s*(\d+)/);
            if (match) {
                const clients = parseInt(match[1]);
                const waiters = parseInt(match[2]);
                const cap = parseInt(match[3]);
                barCapacidadeText.textContent = cap;
                initializeBarActors(clients, waiters);
            }
        }
        // Cliente faz pedido
        else if (line.includes('fez um pedido.')) {
            const match = line.match(/Cliente\s*(\d+)\s*fez um pedido\./);
            if (match) {
                const cId = parseInt(match[1]);
                updateClientState(cId, 'ordered', 'Esperando');
            }
        }
        // Garçom leva pedidos à copa
        else if (line.includes('levando') && line.includes('pedidos para a copa')) {
            const match = line.match(/Garcom\s*(\d+)\s*levando\s*(\d+)\s*pedidos para a copa/);
            if (match) {
                const wId = parseInt(match[1]);
                const count = parseInt(match[2]);
                updateWaiterState(wId, 'copa', `Copa (${count} ped.)`);
                // Anima tokens na fila de pedidos da copa
                for (let i = 0; i < count; i++) {
                    const token = document.createElement('span');
                    token.className = 'order-token';
                    token.textContent = `G${wId}`;
                    barOrdersQueue.appendChild(token);
                }
            }
        }
        // Garçom entrega pedidos
        else if (line.includes('entregando pedidos:')) {
            const match = line.match(/Garcom\s*(\d+)\s*entregando pedidos:\s*\[(.+)\]/);
            if (match) {
                const wId = parseInt(match[1]);
                const clientsStr = match[2];
                const clientIds = clientsStr.split(',').map(s => parseInt(s.trim()));
                
                updateWaiterState(wId, 'active-waiter', `Servindo: [${clientsStr}]`);
                
                // Limpa alguns tokens de pedidos correspondentes na copa
                for (let i = 0; i < clientIds.length; i++) {
                    if (barOrdersQueue.firstElementChild) {
                        barOrdersQueue.removeChild(barOrdersQueue.firstElementChild);
                    }
                }

                // Define clientes como servidos
                clientIds.forEach(cId => {
                    updateClientState(cId, 'served', 'Consumindo');
                    setTimeout(() => {
                        if (isRunning && currentExerciseId === 'bar') {
                            updateClientState(cId, 'thinking', 'Pensando');
                        }
                    }, 3000);
                });
            }
        }
        // Incrementa rodada / Fechamento
        else if (line.includes('!!! O BAR ESTA FECHANDO !!!')) {
            const current = parseInt(barRodadaText.textContent.split('/')[0]);
            const max = parseInt(barRodadaText.textContent.split('/')[1]) || 5;
            const next = Math.min(current + 1, max);
            barRodadaText.textContent = `${next}/${max}`;
            
            if (next === max) {
                appendLog('[Interface] O bar foi fechado oficialmente!', 'system');
                document.querySelectorAll('.client-node').forEach(node => {
                    node.className = 'client-node';
                    node.querySelector('.status-text').textContent = 'Foi embora';
                });
                document.querySelectorAll('.waiter-node').forEach(node => {
                    node.className = 'waiter-node';
                    node.querySelector('.waiter-status').textContent = 'Turno encerrado';
                });
            }
        }
    }

    function initializeBarActors(numClients, numWaiters) {
        barClientsContainer.innerHTML = '';
        barWaitersContainer.innerHTML = '';
        barOrdersQueue.innerHTML = '';
        barRodadaText.textContent = '0/5';

        for (let i = 0; i < numClients; i++) {
            const div = document.createElement('div');
            div.className = 'client-node thinking';
            div.id = `client-${i}`;
            div.innerHTML = `
                <span class="name">Cliente ${i}</span>
                <span class="status-text">Pensando</span>
            `;
            barClientsContainer.appendChild(div);
        }

        for (let i = 0; i < numWaiters; i++) {
            const div = document.createElement('div');
            div.className = 'waiter-node';
            div.id = `waiter-${i}`;
            div.innerHTML = `
                <span class="waiter-name">Garçom ${i}</span>
                <span class="waiter-status">Aguardando clientes</span>
            `;
            barWaitersContainer.appendChild(div);
        }
    }

    function updateClientState(id, cssClass, statusText) {
        const el = document.getElementById(`client-${id}`);
        if (el) {
            el.className = `client-node ${cssClass}`;
            el.querySelector('.status-text').textContent = statusText;
        }
    }

    function updateWaiterState(id, cssClass, statusText) {
        const el = document.getElementById(`waiter-${id}`);
        if (el) {
            el.className = `waiter-node ${cssClass}`;
            el.querySelector('.waiter-status').textContent = statusText;
        }
    }

    // ==========================================
    // 2. PARSER: BARBEIRO DORMINHOCO
    // ==========================================
    function parseBarbeariaLine(line) {
        // Cliente chegou
        if (line.match(/Cliente\s+(\S+)\s+chegou\./)) {
            const match = line.match(/Cliente\s+(\S+)\s+chegou\./);
            addBarberFlowLine(`Cliente ${match[1]} chegou no estabelecimento`, 'in');
        }
        // Sentou na cadeira de espera
        else if (line.match(/Cliente\s+(\S+)\s+sentou na cadeira de espera\.\s*\(Total:\s*(\d+)\)/)) {
            const match = line.match(/Cliente\s+(\S+)\s+sentou na cadeira de espera\.\s*\(Total:\s*(\d+)\)/);
            const count = parseInt(match[2]);
            updateWaitingChairs(count);
            addBarberFlowLine(`Cliente ${match[1]} aguardando na sala de espera`, 'wait');
        }
        // Cortando cabelo
        else if (line.match(/Cliente\s+(\S+)\s+esta cortando o cabelo\./)) {
            const match = line.match(/Cliente\s+(\S+)\s+esta cortando o cabelo\./);
            const cId = match[1];
            chairClient.textContent = `Cliente ${cId}`;
            chairClient.className = 'chair-slot occupied-chair';
            
            // Ajusta o número de cadeiras ocupadas
            const currentChairs = document.querySelectorAll('#waiting-chairs .chair-spot-visual.occupied').length;
            updateWaitingChairs(Math.max(0, currentChairs - 1));
            
            addBarberFlowLine(`Cortando o cabelo do Cliente ${cId}`, 'in');
        }
        // Barbeiro Dormindo
        else if (line.includes('Barbeiro esta dormindo')) {
            barbeiroActor.className = 'actor sleeping';
            barbeiroActor.querySelector('.actor-status-label').textContent = 'Dormindo';
            chairClient.textContent = 'Cadeira Vazia';
            chairClient.className = 'chair-slot empty';
            updateWaitingChairs(0);
        }
        // Barbeiro Acordou
        else if (line.includes('Barbeiro acordou')) {
            barbeiroActor.className = 'actor awake';
            barbeiroActor.querySelector('.actor-status-label').textContent = 'Atendendo';
        }
        // Terminou corte
        else if (line.includes('Barbeiro terminou o corte')) {
            chairClient.textContent = 'Cadeira Vazia';
            chairClient.className = 'chair-slot empty';
        }
        // Barbearia Lotada
        else if (line.includes('Barbearia lotada')) {
            const match = line.match(/Cliente\s+(\S+)\s+foi embora\./);
            if (match) {
                addBarberFlowLine(`Cliente ${match[1]} foi embora (Sem vagas)`, 'away');
            }
        }
    }

    function updateWaitingChairs(count) {
        const container = document.getElementById('waiting-chairs');
        if (!container) return;
        let currentChairs = container.querySelectorAll('.chair-spot-visual');
        while (currentChairs.length < count) {
            const newChair = document.createElement('div');
            newChair.className = 'chair-spot-visual';
            newChair.setAttribute('data-index', currentChairs.length);
            container.appendChild(newChair);
            currentChairs = container.querySelectorAll('.chair-spot-visual');
        }
        const titleEl = document.querySelector('.waiting-room h4');
        if (titleEl) {
            titleEl.textContent = `Sala de Espera (Cadeiras: ${currentChairs.length})`;
        }
        currentChairs.forEach((chair, index) => {
            if (index < count) {
                chair.classList.add('occupied');
            } else {
                chair.classList.remove('occupied');
            }
        });
    }

    function addBarberFlowLine(text, cssClass) {
        const div = document.createElement('div');
        div.className = `flow-line ${cssClass}`;
        div.textContent = `[${new Date().toLocaleTimeString()}] ${text}`;
        barberClientsList.appendChild(div);
        barberClientsList.scrollTop = barberClientsList.scrollHeight;
    }

    // ==========================================
    // 3. PARSER: O JANTAR DOS FILÓSOFOS
    // ==========================================
    function getHeldHashis(philId, state) {
        if (state === 'meditating') {
            return [];
        }
        if (philId === 4) {
            // F4 inverte a ordem para evitar deadlock: pega H0 primeiro, depois H4
            if (state === 'trying') {
                return [0];
            } else if (state === 'eating') {
                return [0, 4];
            }
        } else {
            // Filósofos 0, 1, 2, 3 pegam H_id primeiro, depois H_{id+1}
            if (state === 'trying') {
                return [philId];
            } else if (state === 'eating') {
                return [philId, (philId + 1) % 5];
            }
        }
        return [];
    }

    function syncFilosofosUI() {
        const hashiHolders = { 0: null, 1: null, 2: null, 3: null, 4: null };
        
        for (let i = 0; i < 5; i++) {
            const state = philosopherStates[i];
            const held = getHeldHashis(i, state);
            held.forEach(h => {
                hashiHolders[h] = i;
            });
        }

        for (let i = 0; i < 5; i++) {
            const state = philosopherStates[i];
            let statusText = 'Meditando';
            let heldText = 'Sem hashis';
            let cssClass = 'meditating';
            
            if (state === 'trying') {
                statusText = 'Aguardando';
                const held = getHeldHashis(i, state);
                heldText = `Pego: H${held[0]}`;
                cssClass = 'trying';
            } else if (state === 'eating') {
                statusText = 'Comendo';
                const held = getHeldHashis(i, state);
                heldText = `Pegos: H${held[0]}, H${held[1]}`;
                cssClass = 'eating';
            }
            
            updatePhilosopherCardUI(i, cssClass, statusText, heldText);
        }

        for (let h = 0; h < 5; h++) {
            const holder = hashiHolders[h];
            if (holder !== null) {
                updateHashiElementUI(h, `Em uso (F${holder})`);
            } else {
                updateHashiElementUI(h, 'Livre');
            }
        }
    }

    function parseFilosofosLine(line) {
        // Meditando
        if (line.match(/Filosofo\s+(\d+):\s+Meditando\.\.\./)) {
            const match = line.match(/Filosofo\s+(\d+):\s+Meditando\.\.\./);
            const id = parseInt(match[1]);
            philosopherStates[id] = 'meditating';
            syncFilosofosUI();
        }
        // Tentando comer (pegou esquerdo)
        else if (line.match(/Filosofo\s+(\d+):\s+Pegou hachi esquerdo\.\s*Tentando o direito\.\.\./)) {
            const match = line.match(/Filosofo\s+(\d+):\s+Pegou hachi esquerdo\.\s*Tentando o direito\.\.\./);
            const id = parseInt(match[1]);
            philosopherStates[id] = 'trying';
            syncFilosofosUI();
        }
        // Comendo
        else if (line.match(/Filosofo\s+(\d+):\s+Comendo arroz\.\.\./)) {
            const match = line.match(/Filosofo\s+(\d+):\s+Comendo arroz\.\.\./);
            const id = parseInt(match[1]);
            philosopherStates[id] = 'eating';
            syncFilosofosUI();
        }
    }

    function updatePhilosopherCardUI(id, cssClass, status, hashis) {
        let card = document.getElementById(`phil-${id}`);
        if (!card) {
            const ring = document.getElementById('philosophers-ring');
            if (ring) {
                card = document.createElement('div');
                card.className = `phil-circle-node ${cssClass}`;
                card.id = `phil-${id}`;
                card.setAttribute('data-id', id);
                card.innerHTML = `
                    <span class="phil-name">F${id}</span>
                    <span class="phil-status">${status}</span>
                    <div class="phil-held-hashis">${hashis}</div>
                `;
                ring.appendChild(card);
                repositionPhilosophersRing();
            }
        } else {
            card.className = `phil-circle-node ${cssClass}`;
            card.querySelector('.phil-status').textContent = status;
            card.querySelector('.phil-held-hashis').textContent = hashis;
        }
    }

    function updateHashiElementUI(hashiId, statusText) {
        // Atualiza lista auxiliar
        let listEl = document.getElementById(`hashi-list-item-${hashiId}`);
        if (!listEl) {
            const list = document.getElementById('hashis-list');
            if (list) {
                listEl = document.createElement('div');
                listEl.className = 'hashi-item';
                listEl.id = `hashi-list-item-${hashiId}`;
                listEl.innerHTML = `Hashi ${hashiId}: <span class="status-val text-success">Livre</span>`;
                list.appendChild(listEl);
            }
        }
        if (listEl) {
            const valSpan = listEl.querySelector('.status-val');
            valSpan.textContent = statusText;
            if (statusText === 'Livre') {
                valSpan.className = 'status-val text-success';
            } else {
                valSpan.className = 'status-val text-danger';
            }
        }

        // Atualiza palito de hashi visual
        let stick = document.getElementById(`hashi-stick-${hashiId}`);
        if (!stick) {
            const ring = document.getElementById('philosophers-ring');
            if (ring) {
                stick = document.createElement('div');
                stick.className = 'hashi-stick free';
                stick.id = `hashi-stick-${hashiId}`;
                stick.setAttribute('data-id', hashiId);
                ring.appendChild(stick);
                repositionPhilosophersRing();
            }
        }
        if (stick) {
            if (statusText === 'Livre') {
                stick.className = 'hashi-stick free';
            } else {
                stick.className = 'hashi-stick in-use';
            }
        }
    }

    // ==========================================
    // 4. PARSER: PROBLEMA DAS ROLETAS
    // ==========================================
    function parseRoletasLine(line) {
        if (line.startsWith('Total de pessoas')) {
            const match = line.match(/Total de pessoas que passaram pelas\s+(\d+)\s+roletas:\s+(\d+)/);
            if (match) {
                const numRoletas = parseInt(match[1]);
                const count = parseInt(match[2]);
                roletasContador.textContent = count;
                
                const grid = document.getElementById('roletas-grid');
                if (grid) {
                    let currentNodes = grid.querySelectorAll('.roleta-node');
                    if (currentNodes.length !== numRoletas) {
                        grid.innerHTML = '';
                        for (let i = 0; i < numRoletas; i++) {
                            const node = document.createElement('div');
                            node.className = 'roleta-node';
                            node.id = `roleta-${i}`;
                            node.innerHTML = `
                                <span class="roleta-name">Roleta ${i}</span>
                                <span class="roleta-status-text">Inativo</span>
                            `;
                            grid.appendChild(node);
                        }
                        currentNodes = grid.querySelectorAll('.roleta-node');
                    }
                    
                    const titleEl = document.querySelector('.roletas-threads-visual h4');
                    if (titleEl) {
                        titleEl.textContent = `Threads de Roletas (${numRoletas} ativas)`;
                    }
                    
                    currentNodes.forEach((node) => {
                        node.classList.add('active-roleta');
                        node.querySelector('.roleta-status-text').textContent = 'Finalizado';
                    });
                }
            }
        } else if (line.startsWith('Esperado:')) {
            const match = line.match(/Esperado:\s*(\d+)/);
            if (match) {
                roletasEsperado.textContent = match[1];
            }
        } else if (line.match(/Roleta\s+(\d+)\s+girou\.\s+Contador:\s+(\d+)/)) {
            const match = line.match(/Roleta\s+(\d+)\s+girou\.\s+Contador:\s+(\d+)/);
            const id = parseInt(match[1]);
            const count = parseInt(match[2]);
            
            roletasContador.textContent = count;
            
            const grid = document.getElementById('roletas-grid');
            if (grid) {
                let node = document.getElementById(`roleta-${id}`);
                if (!node) {
                    node = document.createElement('div');
                    node.className = 'roleta-node';
                    node.id = `roleta-${id}`;
                    node.innerHTML = `
                        <span class="roleta-name">Roleta ${id}</span>
                        <span class="roleta-status-text">Inativo</span>
                    `;
                    grid.appendChild(node);
                    
                    const titleEl = document.querySelector('.roletas-threads-visual h4');
                    if (titleEl) {
                        const activeCount = grid.querySelectorAll('.roleta-node').length;
                        titleEl.textContent = `Threads de Roletas (${activeCount} ativas)`;
                    }
                }
                node.classList.add('active-roleta');
                node.querySelector('.roleta-status-text').textContent = 'Girando...';
            }
        }
    }

    function parseContasLine(line) {
        // Depósitos, saques e juros
        if (line.includes('Depositado') || line.includes('Sacado') || line.includes('creditados')) {
            const match = line.match(/(\S+):\s+([A-Za-z\s]+)\s+([\d\.]+)(?:\.|\s+creditados).*Novo saldo:\s+([\d\.]+)/);
            if (match) {
                const acc = match[1];
                const action = match[2].trim();
                const amount = parseFloat(match[3]);
                const newBal = parseFloat(match[4]);
                
                updateAccountUI(acc, newBal);
                addTransactionItem(`${acc}: ${action} R$ ${amount.toFixed(2)} -> Saldo: R$ ${newBal.toFixed(2)}`);
            }
        }
        // Transferências OK
        else if (line.includes('Transferencia') && line.includes('OK.')) {
            const match = line.match(/Transferencia de\s+([\d\.]+)\s+de\s+(\S+)\s+para\s+(\S+)\s+OK\./);
            if (match) {
                const amount = parseFloat(match[1]);
                const src = match[2];
                const dest = match[3];
                
                addTransactionItem(`Transf. de R$ ${amount.toFixed(2)} de ${src} para ${dest}`);
                
                // Também atualiza os saldos das duas contas na tela
                const balMatch = line.match(/Novo saldo\s+(\S+):\s+([\d\.]+)\s+\|\s+Novo saldo\s+(\S+):\s+([\d\.]+)/);
                if (balMatch) {
                    updateAccountUI(balMatch[1], parseFloat(balMatch[2]));
                    updateAccountUI(balMatch[3], parseFloat(balMatch[4]));
                }
            }
        }
        // Insuficiente
        else if (line.includes('Saldo insuficiente')) {
            const match = line.match(/(\S+):\s+Saldo insuficiente para saque de\s+([\d\.]+)/);
            if (match) {
                const acc = match[1];
                const amount = parseFloat(match[2]);
                addTransactionItem(`${acc}: Saldo insuficiente para saque de R$ ${amount.toFixed(2)}`);
            }
        }
    }

    function updateAccountUI(accountName, balance) {
        const idLower = accountName.toLowerCase();
        let card = document.getElementById(`account-${idLower}`);
        if (!card) {
            const grid = document.querySelector('.accounts-grid');
            if (grid) {
                card = document.createElement('div');
                card.className = 'account-card';
                card.id = `account-${idLower}`;
                card.innerHTML = `
                    <span class="account-label">${accountName.replace('-', ' ')}</span>
                    <span class="account-balance font-mono" id="balance-${idLower}">R$ 0,00</span>
                `;
                grid.appendChild(card);
            }
        }
        const balanceEl = document.getElementById(`balance-${idLower}`);
        if (balanceEl) {
            balanceEl.textContent = `R$ ${balance.toFixed(2)}`;
        }
    }

    function addTransactionItem(text) {
        const empty = transactionsList.querySelector('.empty-log');
        if (empty) empty.remove();
        
        const p = document.createElement('p');
        p.className = 'transaction-item';
        p.textContent = `[${new Date().toLocaleTimeString()}] ${text}`;
        transactionsList.appendChild(p);
        transactionsList.scrollTop = transactionsList.scrollHeight;
    }

    // ==========================================
    // 6. PARSER: PRODUTOR / CONSUMIDOR
    // ==========================================

    function parseProdConsLine(line) {
        // Monitor: Produtor inseriu / Produtor inseriu:
        if (line.includes('inseriu')) {
            const match = line.match(/(?:Monitor:\s+)?Produtor inseriu:?\s*(\d+)/);
            if (match) {
                const item = parseInt(match[1]);
                bufferItems.push(item);
                updateBufferUI();
                
                prodconsProducer.className = 'actor-card active-producer';
                prodconsProducer.querySelector('.actor-state').textContent = `Inseriu item ${item}`;
                
                setTimeout(() => {
                    if (isRunning && currentExerciseId === 'prodcons') {
                        prodconsProducer.className = 'actor-card';
                        prodconsProducer.querySelector('.actor-state').textContent = 'Produzindo...';
                    }
                }, 400);
            }
        }
        // Monitor: Consumidor removeu / Consumidor removeu:
        else if (line.includes('removeu')) {
            const match = line.match(/(?:Monitor:\s+)?Consumidor removeu:?\s*(\d+)/);
            if (match) {
                const item = parseInt(match[1]);
                bufferItems.shift(); // remove o primeiro da fila
                updateBufferUI();

                prodconsConsumer.className = 'actor-card active-consumer';
                prodconsConsumer.querySelector('.actor-state').textContent = `Removeu item ${item}`;
                
                setTimeout(() => {
                    if (isRunning && currentExerciseId === 'prodcons') {
                        prodconsConsumer.className = 'actor-card';
                        prodconsConsumer.querySelector('.actor-state').textContent = 'Consumindo...';
                    }
                }, 400);
            }
        }
    }

    function updateBufferUI() {
        const container = document.getElementById('buffer-slots');
        if (!container) return;
        let slots = container.querySelectorAll('.buffer-slot');
        const requiredSlots = Math.max(5, bufferItems.length);
        
        while (slots.length < requiredSlots) {
            const newSlot = document.createElement('div');
            newSlot.className = 'buffer-slot';
            newSlot.setAttribute('data-index', slots.length);
            newSlot.innerHTML = `<span class="slot-idx">${slots.length}</span><span class="slot-val">-</span>`;
            container.appendChild(newSlot);
            slots = container.querySelectorAll('.buffer-slot');
        }
        
        const titleEl = document.querySelector('.buffer-visual-area h4');
        if (titleEl) {
            titleEl.textContent = `Buffer Compartilhado (Limite: ${slots.length})`;
        }
        
        slots.forEach((slot, i) => {
            const valSpan = slot.querySelector('.slot-val');
            if (i < bufferItems.length) {
                slot.className = 'buffer-slot filled';
                valSpan.textContent = bufferItems[i];
            } else {
                slot.className = 'buffer-slot';
                valSpan.textContent = '-';
            }
        });
    }

    // ==========================================
    // AUXILIARES DE RESET & ESTADOS
    // ==========================================
    function resetAllUIs() {
        // 1. Reset Bar
        barClientsContainer.innerHTML = '<p class="footer-sub">Aguardando início da simulação...</p>';
        barWaitersContainer.innerHTML = '';
        barOrdersQueue.innerHTML = '';
        barRodadaText.textContent = '0/5';
        barCapacidadeText.textContent = '3';

        // 2. Reset Barbearia
        barbeiroActor.className = 'actor sleeping';
        barbeiroActor.querySelector('.actor-status-label').textContent = 'Dormindo';
        chairClient.textContent = 'Cadeira Vazia';
        chairClient.className = 'chair-slot empty';
        const waitingChairsContainer = document.getElementById('waiting-chairs');
        if (waitingChairsContainer) {
            waitingChairsContainer.innerHTML = `
                <div class="chair-spot-visual" data-index="0"></div>
                <div class="chair-spot-visual" data-index="1"></div>
                <div class="chair-spot-visual" data-index="2"></div>
            `;
        }
        const waitingTitle = document.querySelector('.waiting-room h4');
        if (waitingTitle) {
            waitingTitle.textContent = 'Sala de Espera (Cadeiras: 3)';
        }
        barberClientsList.innerHTML = '<p class="empty-log">Aguardando início da simulação...</p>';

        // 3. Reset Filósofos
        const ring = document.getElementById('philosophers-ring');
        if (ring) {
            ring.innerHTML = '';
            for (let i = 0; i < 5; i++) {
                // Adiciona o filósofo
                const card = document.createElement('div');
                card.className = 'phil-circle-node meditating';
                card.id = `phil-${i}`;
                card.setAttribute('data-id', i);
                card.innerHTML = `
                    <span class="phil-name">F${i}</span>
                    <span class="phil-status">Meditando</span>
                    <div class="phil-held-hashis">Sem hashis</div>
                `;
                ring.appendChild(card);

                // Adiciona o hashi stick
                const stick = document.createElement('div');
                stick.className = 'hashi-stick free';
                stick.id = `hashi-stick-${i}`;
                stick.setAttribute('data-id', i);
                ring.appendChild(stick);
            }
        }
        const hashiList = document.getElementById('hashis-list');
        if (hashiList) {
            hashiList.innerHTML = '';
            for (let i = 0; i < 5; i++) {
                hashiList.innerHTML += `
                    <div class="hashi-item" id="hashi-list-item-${i}">Hashi ${i}: <span class="status-val text-success">Livre</span></div>
                `;
            }
        }
        for (let i = 0; i < 5; i++) {
            philosopherStates[i] = 'meditating';
        }
        setTimeout(repositionPhilosophersRing, 0);

        // 4. Reset Roletas
        roletasContador.textContent = '0';
        roletasEsperado.textContent = '5000';
        const roletasGrid = document.getElementById('roletas-grid');
        if (roletasGrid) {
            roletasGrid.innerHTML = '';
            for (let i = 0; i < 5; i++) {
                roletasGrid.innerHTML += `
                    <div class="roleta-node" id="roleta-${i}">
                        <span class="roleta-name">Roleta ${i}</span>
                        <span class="roleta-status-text">Inativo</span>
                    </div>
                `;
            }
        }
        const roletasTitle = document.querySelector('.roletas-threads-visual h4');
        if (roletasTitle) {
            roletasTitle.textContent = 'Threads de Roletas (5 ativas)';
        }

        // 5. Reset Contas
        const accountsGrid = document.querySelector('.accounts-grid');
        if (accountsGrid) {
            accountsGrid.innerHTML = `
                <div class="account-card" id="account-conta-a">
                    <span class="account-label">Conta A</span>
                    <span class="account-balance font-mono" id="balance-conta-a">R$ 1000,00</span>
                </div>
                <div class="account-card" id="account-conta-b">
                    <span class="account-label">Conta B</span>
                    <span class="account-balance font-mono" id="balance-conta-b">R$ 500,00</span>
                </div>
            `;
        }
        transactionsList.innerHTML = '<p class="empty-log">Aguardando início da simulação...</p>';

        // 6. Reset Produtor/Consumidor
        bufferItems = [];
        const bufferContainer = document.getElementById('buffer-slots');
        if (bufferContainer) {
            bufferContainer.innerHTML = '';
            for (let i = 0; i < 5; i++) {
                bufferContainer.innerHTML += `
                    <div class="buffer-slot" data-index="${i}"><span class="slot-idx">${i}</span><span class="slot-val">-</span></div>
                `;
            }
        }
        const bufferTitle = document.querySelector('.buffer-visual-area h4');
        if (bufferTitle) {
            bufferTitle.textContent = 'Buffer Compartilhado (Limite: 5)';
        }
        prodconsProducer.className = 'actor-card';
        prodconsProducer.querySelector('.actor-state').textContent = 'Parado';
        prodconsConsumer.className = 'actor-card';
        prodconsConsumer.querySelector('.actor-state').textContent = 'Parado';

        // 7. Reset Sockets
        const socketContainer = document.getElementById('socket-client-container');
        if (socketContainer) {
            socketContainer.innerHTML = '<div style="font-size: 0.85rem; color: var(--text-muted); padding: 16px; text-align: center;">Selecione um exercício de socket, clique em "Iniciar" acima e interaja por aqui.</div>';
        }
        
        // Clean active socket variables
        currentSocketSessionId = null;
        forcaSessionId = null;
        bankSessionId = null;
        bankAccountNum = null;
        chatSessionId = null;
        activeChatNickname = '';
        integersList = [];
        uploadedFiles = [];
        forcaErros = 0;
    }

    // --- SOCKETS UI RENDERING & LOGIC ---
    function renderSocketClientUI(id) {
        const container = document.getElementById('socket-client-container');
        if (!container) return;

        if (id === 'socket_fortune') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Visualização do Biscoito da Sorte</h4>
                    <div class="fortune-wrapper">
                        <div id="fortune-cookie" class="fortune-cookie-graphic"></div>
                        <div id="fortune-paper" class="fortune-paper">Clique no biscoito para abri-lo e receber sua sorte!</div>
                    </div>
                </div>
                <div class="socket-card">
                    <h4>Gerenciar Banco de Frases (Porta 12345)</h4>
                    <div class="socket-control-row">
                        <button id="btn-fortune-get" class="btn">Obter Frase (GET)</button>
                        <button id="btn-fortune-list" class="btn btn-secondary">Listar Frases (LST)</button>
                    </div>
                    <div class="socket-control-row" style="margin-top: 16px;">
                        <input type="text" id="fortune-add-input" placeholder="Nova frase do biscoito..." style="flex: 1;">
                        <button id="btn-fortune-add" class="btn btn-success">Adicionar (ADD)</button>
                    </div>
                    <div class="socket-control-row" style="margin-top: 12px;">
                        <input type="number" id="fortune-upd-index" placeholder="Posição" style="width: 80px;">
                        <input type="text" id="fortune-upd-input" placeholder="Novo texto..." style="flex: 1;">
                        <button id="btn-fortune-upd" class="btn btn-warning">Editar (UPD)</button>
                    </div>
                </div>
            `;

            document.getElementById('fortune-cookie').addEventListener('click', getFortuneCookie);
            document.getElementById('btn-fortune-get').addEventListener('click', getFortuneCookie);
            document.getElementById('btn-fortune-list').addEventListener('click', listFortunes);
            document.getElementById('btn-fortune-add').addEventListener('click', addFortune);
            document.getElementById('btn-fortune-upd').addEventListener('click', updateFortune);
        }
        else if (id === 'socket_inteiros') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Calculadora de Inteiros Concorrente (Porta 12346)</h4>
                    <p style="font-size: 0.8rem; margin-bottom: 12px; color: var(--text-secondary);">
                        Insira números e selecione a operação. O envio finaliza com EOF para disparar o cálculo no servidor.
                    </p>
                    <div class="socket-control-row">
                        <input type="number" id="inteiros-number-input" placeholder="Digite um número..." style="flex: 1;">
                        <button id="btn-inteiros-add-list" class="btn">Adicionar</button>
                    </div>
                    
                    <div style="margin: 12px 0;">
                        <strong>Números na Lista:</strong>
                        <div id="inteiros-list-display" class="file-list-box" style="margin-top: 6px; min-height: 80px; max-height: 100px; padding: 6px;">
                            <div style="font-size: 0.75rem; color: var(--text-muted); padding: 8px;">Nenhum número adicionado.</div>
                        </div>
                    </div>

                    <div class="socket-control-row">
                        <label class="radio-label">
                            <input type="radio" name="inteiros-op" value="SOMA" checked> <span>Soma (+)</span>
                        </label>
                        <label class="radio-label" style="margin-left: 12px;">
                            <input type="radio" name="inteiros-op" value="MULT"> <span>Multiplicação (×)</span>
                        </label>
                    </div>

                    <div class="socket-control-row" style="margin-top: 16px;">
                        <button id="btn-inteiros-process" class="btn btn-success" style="flex: 1;">Enviar para Processamento (EOF)</button>
                        <button id="btn-inteiros-clear" class="btn btn-secondary">Limpar Lista</button>
                    </div>
                    
                    <div style="margin-top: 16px; display: flex; align-items: center; justify-content: space-between; padding: 12px; background: rgba(0,0,0,0.2); border-radius: 6px;">
                        <span>Resultado da Operação:</span>
                        <strong id="inteiros-result" class="font-mono text-success" style="font-size: 1.2rem;">-</strong>
                    </div>
                </div>
            `;

            document.getElementById('btn-inteiros-add-list').addEventListener('click', addIntegerToList);
            document.getElementById('btn-inteiros-process').addEventListener('click', processIntegers);
            document.getElementById('btn-inteiros-clear').addEventListener('click', clearIntegersList);
        }
        else if (id === 'socket_forca') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Jogo da Forca Remoto (Porta 12347)</h4>
                    <div class="forca-layout">
                        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; background: rgba(0,0,0,0.15); border-radius: 6px; padding: 12px;">
                            <svg width="100" height="150" id="forca-svg" style="stroke: var(--text-primary); stroke-width: 3; fill: none;">
                                <line x1="10" y1="140" x2="90" y2="140" />
                                <line x1="30" y1="140" x2="30" y2="20" />
                                <line x1="30" y1="20" x2="70" y2="20" />
                                <line x1="70" y1="20" x2="70" y2="40" />
                                <circle cx="70" cy="50" r="10" id="hang-head" style="display: none;" />
                                <line x1="70" y1="60" x2="70" y2="100" id="hang-body" style="display: none;" />
                                <line x1="70" y1="70" x2="50" y2="90" id="hang-l-arm" style="display: none;" />
                                <line x1="70" y1="70" x2="90" y2="90" id="hang-r-arm" style="display: none;" />
                                <line x1="70" y1="100" x2="55" y2="130" id="hang-l-leg" style="display: none;" />
                                <line x1="70" y1="100" x2="85" y2="130" id="hang-r-leg" style="display: none;" />
                            </svg>
                            <div style="margin-top: 10px; font-size: 0.75rem; color: var(--text-secondary);" id="forca-erros-counter">Erros: 0/6</div>
                        </div>
                        <div>
                            <div class="forca-word-display" id="forca-word">_ _ _ _ _ _</div>
                            <div class="forca-kbd" id="forca-keyboard"></div>
                            <div class="forca-status-msg" id="forca-status">Aguardando conexão... Clique em Conectar e Jogar!</div>
                            <button id="btn-forca-play" class="btn btn-success" style="width: 100%; margin-top: 16px;">Conectar e Jogar</button>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById('btn-forca-play').addEventListener('click', connectForcaGame);
        }
        else if (id === 'socket_banco') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Cliente de Instituição Financeira (Porta 12348)</h4>
                    
                    <div id="bank-login-panel" class="bank-login-card">
                        <label style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 4px;">Insira o número da conta para conectar:</label>
                        <input type="text" id="bank-account-number" placeholder="Ex: 123 ou 456" style="text-align: center; font-size: 1.1rem; width: 180px; margin-bottom: 8px;">
                        <button id="btn-bank-login" class="btn btn-success" style="width: 180px;">Entrar na Conta</button>
                    </div>

                    <div id="bank-dashboard-panel" class="bank-dashboard" style="display: none;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <span class="bank-acc-info">Conta Ativa</span>
                                <div id="bank-active-acc-label" style="font-weight: bold; font-size: 1rem;">N/A</div>
                            </div>
                            <button id="btn-bank-logout" class="btn btn-mini btn-danger">Sair</button>
                        </div>
                        
                        <div style="margin: 20px 0; text-align: center;">
                            <span class="bank-acc-info">Saldo Disponível</span>
                            <div id="bank-balance-display" class="bank-balance">R$ 0,00</div>
                        </div>

                        <div style="border-top: 1px solid rgba(255,255,255,0.05); padding-top: 16px;">
                            <div class="socket-control-row">
                                <input type="number" id="bank-amount-input" placeholder="Valor (R$)" style="flex: 1; background: rgba(0,0,0,0.3); color: white; border: 1px solid var(--border-strong);">
                            </div>
                            <div class="bank-grid-actions">
                                <button id="btn-bank-get-balance" class="btn btn-secondary">Atualizar Saldo</button>
                                <button id="btn-bank-deposit" class="btn btn-success">Depositar</button>
                                <button id="btn-bank-withdraw" class="btn btn-warning">Sacar</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById('btn-bank-login').addEventListener('click', connectBank);
            document.getElementById('btn-bank-logout').addEventListener('click', disconnectBank);
            document.getElementById('btn-bank-get-balance').addEventListener('click', () => sendBankCommand('1'));
            document.getElementById('btn-bank-deposit').addEventListener('click', () => {
                const val = document.getElementById('bank-amount-input').value;
                sendBankCommand(`2 ${val}`);
            });
            document.getElementById('btn-bank-withdraw').addEventListener('click', () => {
                const val = document.getElementById('bank-amount-input').value;
                sendBankCommand(`3 ${val}`);
            });
        }
        else if (id === 'socket_lojas') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Rede de Lojas - Dashboard de Integração (Porta 12349)</h4>
                    <p style="font-size: 0.8rem; margin-bottom: 12px; color: var(--text-secondary);">
                        Inicie o Sistema Central (Servidor) e depois dispare as filiais. A central somará em tempo real todas as transações concorrentes.
                    </p>
                    <div class="stores-dashboard">
                        <div>
                            <h5 style="font-size: 0.75rem; margin-bottom: 8px; text-transform: uppercase; color: var(--text-muted);">Filiais Ativas</h5>
                            <div class="filial-cards-container">
                                <div class="filial-card-node" id="filial-card-0">
                                    <h5>Filial 0</h5>
                                    <div class="filial-val" id="filial-val-0">R$ 0,00</div>
                                </div>
                                <div class="filial-card-node" id="filial-card-1">
                                    <h5>Filial 1</h5>
                                    <div class="filial-val" id="filial-val-1">R$ 0,00</div>
                                </div>
                                <div class="filial-card-node" id="filial-card-2">
                                    <h5>Filial 2</h5>
                                    <div class="filial-val" id="filial-val-2">R$ 0,00</div>
                                </div>
                                <div class="filial-card-node" id="filial-card-3">
                                    <h5>Filial 3</h5>
                                    <div class="filial-val" id="filial-val-3">R$ 0,00</div>
                                </div>
                            </div>
                        </div>
                        <div style="display: flex; flex-direction: column; justify-content: center; align-items: center; background: rgba(0,0,0,0.15); border-radius: 6px; padding: 16px;">
                            <span style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-secondary);">TOTAL ACUMULADO</span>
                            <div id="central-total-sales" style="font-size: 1.8rem; font-weight: bold; color: var(--accent-success); margin-top: 8px; font-family: var(--font-mono);">R$ 0,00</div>
                            <button id="btn-stores-trigger-clients" class="btn btn-success" style="margin-top: 16px; width: 100%;">Disparar 4 Filiais (Clientes)</button>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById('btn-stores-trigger-clients').addEventListener('click', triggerStoresClients);
        }
        else if (id === 'socket_arquivos') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Servidor de Arquivos (Porta 12350)</h4>
                    <div class="files-layout">
                        <div>
                            <h5 style="font-size: 0.75rem; margin-bottom: 8px; text-transform: uppercase; color: var(--text-muted);">Área Local (Enviar)</h5>
                            <div class="file-list-box">
                                <div class="file-item-row">
                                    <span class="file-name-label">contrato.txt</span>
                                    <button class="btn btn-mini btn-success btn-upload" data-name="contrato.txt" data-content="Este é um arquivo de teste de contrato de prestação de serviços.">Upload</button>
                                </div>
                                <div class="file-item-row">
                                    <span class="file-name-label">relatorio.txt</span>
                                    <button class="btn btn-mini btn-success btn-upload" data-name="relatorio.txt" data-content="Relatório de Vendas: Filial 1 faturou R$ 15.000, Filial 2 faturou R$ 12.000.">Upload</button>
                                </div>
                                <div class="file-item-row">
                                    <span class="file-name-label">dados.json</span>
                                    <button class="btn btn-mini btn-success btn-upload" data-name="dados.json" data-content='{"status": "OK", "timestamp": 129038012}'>Upload</button>
                                </div>
                            </div>
                        </div>
                        <div>
                            <h5 style="font-size: 0.75rem; margin-bottom: 8px; text-transform: uppercase; color: var(--text-muted);">Arquivos no Servidor</h5>
                            <div class="file-list-box" id="server-files-list">
                                <div style="font-size: 0.75rem; color: var(--text-muted); text-align: center; margin-top: 20px;">Nenhum arquivo no servidor.</div>
                            </div>
                        </div>
                    </div>
                    
                    <div id="file-content-preview-card" style="margin-top: 16px; padding: 12px; background: rgba(0,0,0,0.25); border-radius: 6px; display: none;">
                        <strong>Visualizando Arquivo Baixado (<span id="downloaded-filename"></span>):</strong>
                        <pre id="downloaded-file-content" style="margin-top: 6px; font-size: 0.75rem; color: #a1a1aa; white-space: pre-wrap; word-break: break-all; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 6px;"></pre>
                    </div>
                </div>
            `;

            container.querySelectorAll('.btn-upload').forEach(btn => {
                btn.addEventListener('click', () => {
                    uploadFile(btn.getAttribute('data-name'), btn.getAttribute('data-content'));
                });
            });

            renderServerFiles();
        }
        else if (id === 'socket_chat') {
            container.innerHTML = `
                <div class="socket-card">
                    <h4>Chat Multicast UTFPR (Grupo: 224.0.0.1 - Porta 12351)</h4>
                    
                    <div id="chat-join-panel" class="chat-welcome-card">
                        <label style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 4px;">Escolha um Apelido para Entrar no Chat:</label>
                        <input type="text" id="chat-nickname-input" placeholder="Ex: Maria" style="width: 200px; text-align: center; margin-bottom: 8px;">
                        <button id="btn-chat-join" class="btn btn-success" style="width: 200px;">Entrar no Grupo</button>
                    </div>

                    <div id="chat-active-panel" class="chat-layout" style="display: none;">
                        <div id="chat-messages" class="chat-messages-container"></div>
                        <div class="chat-input-bar">
                            <input type="text" id="chat-message-input" placeholder="Digite sua mensagem...">
                            <button id="btn-chat-send" class="btn btn-success">Enviar</button>
                            <button id="btn-chat-leave" class="btn btn-danger">Sair</button>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById('btn-chat-join').addEventListener('click', joinChat);
            document.getElementById('btn-chat-leave').addEventListener('click', leaveChat);
            document.getElementById('btn-chat-send').addEventListener('click', sendChatMessage);
            document.getElementById('chat-message-input').addEventListener('keydown', (e) => {
                if (e.key === 'Enter') sendChatMessage();
            });
        }
    }

    // --- FORTUNE COOKIE HANDLERS ---
    async function getFortuneCookie() {
        if (fortuneTimeout) {
            clearTimeout(fortuneTimeout);
            fortuneTimeout = null;
        }

        const cookie = document.getElementById('fortune-cookie');
        const paper = document.getElementById('fortune-paper');
        if (cookie) cookie.classList.remove('cracked');
        if (paper) paper.classList.remove('show');

        const sessionId = 'fortune_' + Date.now();
        currentSocketSessionId = sessionId;

        appendLog(`[Biscoito] Conectando ao Servidor Fortune...`, 'system');
        const connRes = await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, port: 12345 })
        });
        const connData = await connRes.json();
        if (!connData.success) {
            appendLog(`[Biscoito Erro] Falha na conexão: ${connData.error}`, 'stderr');
            return;
        }

        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, data: 'GET-FORTUNE\n' })
        });
    }

    function handleFortuneData(text) {
        appendLog(`[Biscoito] Resposta: ${text}`);
        
        // Se for listagem ou outra resposta, não quebra biscoito
        if (text.startsWith('[') || text.includes('Adicionado') || text.includes('Editado')) {
            return;
        }

        const paper = document.getElementById('fortune-paper');
        if (paper) {
            paper.textContent = text;
            paper.classList.add('show');
        }
        const cookie = document.getElementById('fortune-cookie');
        if (cookie) {
            cookie.classList.add('cracked');
        }
        if (currentSocketSessionId) {
            fetch('/api/socket/disconnect', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionId: currentSocketSessionId })
            }).catch(() => {});
            currentSocketSessionId = null;
        }

        // Fecha automaticamente o biscoito após 5 segundos
        fortuneTimeout = setTimeout(() => {
            if (cookie) cookie.classList.remove('cracked');
            if (paper) paper.classList.remove('show');
            fortuneTimeout = null;
        }, 5000);
    }

    async function listFortunes() {
        const sessionId = 'fortune_' + Date.now();
        currentSocketSessionId = sessionId;
        appendLog(`[Biscoito] Solicitando lista de frases...`, 'system');
        await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, port: 12345 })
        });
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, data: 'LST-FORTUNE\n', closeWrite: true })
        });
    }

    async function addFortune() {
        const val = document.getElementById('fortune-add-input').value.trim();
        if (!val) return;
        const sessionId = 'fortune_' + Date.now();
        currentSocketSessionId = sessionId;
        appendLog(`[Biscoito] Adicionando nova frase...`, 'system');
        await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, port: 12345 })
        });
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, data: `ADD-FORTUNE ${val}\n`, closeWrite: true })
        });
        document.getElementById('fortune-add-input').value = '';
    }

    async function updateFortune() {
        const idx = document.getElementById('fortune-upd-index').value.trim();
        const val = document.getElementById('fortune-upd-input').value.trim();
        if (!idx || !val) return;
        const sessionId = 'fortune_' + Date.now();
        currentSocketSessionId = sessionId;
        appendLog(`[Biscoito] Atualizando frase na posição ${idx}...`, 'system');
        await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, port: 12345 })
        });
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, data: `UPD-FORTUNE ${idx};${val}\n`, closeWrite: true })
        });
        document.getElementById('fortune-upd-index').value = '';
        document.getElementById('fortune-upd-input').value = '';
    }

    // --- INTEIROS HANDLERS ---
    function addIntegerToList() {
        const input = document.getElementById('inteiros-number-input');
        const num = parseFloat(input.value);
        if (isNaN(num)) return;
        integersList.push(num);
        input.value = '';
        renderIntegersList();
    }

    function renderIntegersList() {
        const listDisplay = document.getElementById('inteiros-list-display');
        if (!listDisplay) return;
        if (integersList.length === 0) {
            listDisplay.innerHTML = '<div style="font-size: 0.75rem; color: var(--text-muted); padding: 8px;">Nenhum número adicionado.</div>';
            return;
        }
        listDisplay.innerHTML = integersList.map((num, idx) => `
            <div class="file-item-row" style="margin-bottom: 4px;">
                <span class="file-name-label">Número [${idx}]: <strong>${num}</strong></span>
                <button class="btn btn-mini btn-danger" id="btn-remove-int-${idx}">Remover</button>
            </div>
        `).join('');

        // Bind delete events
        integersList.forEach((_, idx) => {
            document.getElementById(`btn-remove-int-${idx}`).addEventListener('click', () => {
                integersList.splice(idx, 1);
                renderIntegersList();
            });
        });
    }

    function clearIntegersList() {
        integersList = [];
        renderIntegersList();
        document.getElementById('inteiros-result').textContent = '-';
    }

    async function processIntegers() {
        if (integersList.length === 0) {
            appendLog(`[Inteiros Erro] Adicione ao menos um número!`, 'stderr');
            return;
        }
        const sessionId = 'inteiros_' + Date.now();
        currentSocketSessionId = sessionId;
        appendLog(`[Inteiros] Enviando inteiros para o processamento...`, 'system');
        
        await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, port: 12346 })
        });

        for (let i = 0; i < integersList.length; i++) {
            await fetch('/api/socket/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionId, data: integersList[i] + '\n' })
            });
        }

        const op = document.querySelector('input[name="inteiros-op"]:checked').value;
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId, data: op + '\n', closeWrite: true })
        });
    }

    function handleInteirosData(text) {
        appendLog(`[Inteiros] Resultado: ${text}`);
        const display = document.getElementById('inteiros-result');
        if (display) {
            display.textContent = text.replace('RESULTADO DA OPERACAO:', '').trim();
        }
    }

    // --- FORCA GAME HANDLERS ---
    async function connectForcaGame() {
        forcaSessionId = 'forca_' + Date.now();
        forcaErros = 0;
        
        document.getElementById('hang-head').style.display = 'none';
        document.getElementById('hang-body').style.display = 'none';
        document.getElementById('hang-l-arm').style.display = 'none';
        document.getElementById('hang-r-arm').style.display = 'none';
        document.getElementById('hang-l-leg').style.display = 'none';
        document.getElementById('hang-r-leg').style.display = 'none';
        
        document.getElementById('forca-erros-counter').textContent = 'Erros: 0/6';
        document.getElementById('forca-status').textContent = 'Conectando ao jogo da forca...';

        const connRes = await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: forcaSessionId, port: 12347 })
        });
        const connData = await connRes.json();
        if (!connData.success) {
            document.getElementById('forca-status').textContent = 'Erro ao conectar no servidor de Forca.';
            appendLog(`[Forca Erro] ${connData.error}`, 'stderr');
            return;
        }

        renderForcaKeyboard();
        document.getElementById('btn-forca-play').disabled = true;
    }

    function renderForcaKeyboard() {
        const kbd = document.getElementById('forca-keyboard');
        if (!kbd) return;
        const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        kbd.innerHTML = alphabet.split('').map(letter => `
            <button class="forca-key" id="forca-key-${letter}" data-letter="${letter}">${letter}</button>
        `).join('');

        kbd.querySelectorAll('.forca-key').forEach(btn => {
            btn.addEventListener('click', () => {
                btn.disabled = true;
                const letter = btn.getAttribute('data-letter');
                sendForcaLetter(letter);
            });
        });
    }

    async function sendForcaLetter(letter) {
        if (!forcaSessionId) return;
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: forcaSessionId, data: letter + '\n' })
        });
    }

    function handleForcaData(text) {
        appendLog(`[Forca] ${text}`);
        const statusEl = document.getElementById('forca-status');
        const wordEl = document.getElementById('forca-word');
        const errorsEl = document.getElementById('forca-erros-counter');

        const lines = text.split('\n');
        lines.forEach(line => {
            line = line.trim();
            if (!line) return;

            if (line.startsWith('BEM-VINDO')) {
                statusEl.textContent = 'Jogo iniciado! Adivinhe a palavra.';
            } else if (line.startsWith('TAMANHO:')) {
                const len = parseInt(line.split(':')[1]);
                wordEl.textContent = '_ '.repeat(len).trim();
            } else if (line.includes('ESTADO:')) {
                const stateMatch = line.match(/ESTADO:(.+)\s+ERROS:(\d+)\/(\d+)/);
                if (stateMatch) {
                    const state = stateMatch[1].trim();
                    const erros = parseInt(stateMatch[2]);
                    wordEl.textContent = state;
                    errorsEl.textContent = `Erros: ${erros}/6`;
                    
                    forcaErros = erros;
                    if (erros >= 1) document.getElementById('hang-head').style.display = 'block';
                    if (erros >= 2) document.getElementById('hang-body').style.display = 'block';
                    if (erros >= 3) document.getElementById('hang-l-arm').style.display = 'block';
                    if (erros >= 4) document.getElementById('hang-r-arm').style.display = 'block';
                    if (erros >= 5) document.getElementById('hang-l-leg').style.display = 'block';
                    if (erros >= 6) document.getElementById('hang-r-leg').style.display = 'block';
                }
            } else if (line.startsWith('AVISO:')) {
                statusEl.textContent = line.replace('AVISO:', '').trim();
            } else if (line.startsWith('VITORIA:')) {
                statusEl.innerHTML = `<span style="color: var(--accent-success); font-weight:bold;">${line}</span>`;
                disableForcaKeyboard();
            } else if (line.startsWith('DERROTA:')) {
                statusEl.innerHTML = `<span style="color: var(--accent-danger); font-weight:bold;">${line}</span>`;
                document.getElementById('hang-l-leg').style.display = 'block';
                document.getElementById('hang-r-leg').style.display = 'block';
                disableForcaKeyboard();
            }
        });
    }

    function disableForcaKeyboard() {
        document.querySelectorAll('.forca-key').forEach(btn => btn.disabled = true);
        document.getElementById('btn-forca-play').disabled = false;
        forcaSessionId = null;
    }

    function handleForcaClose() {
        disableForcaKeyboard();
    }

    // --- BANCO HANDLERS ---
    async function connectBank() {
        const accInput = document.getElementById('bank-account-number');
        const account = accInput.value.trim();
        if (!account) return;

        bankAccountNum = account;
        bankSessionId = 'banco_' + Date.now();

        appendLog(`[Banco] Conectando e acessando conta ${account}...`, 'system');
        const connRes = await fetch('/api/socket/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: bankSessionId, port: 12348 })
        });
        const connData = await connRes.json();
        if (!connData.success) {
            appendLog(`[Banco Erro] Falha na conexão: ${connData.error}`, 'stderr');
            return;
        }
    }

    async function sendBankCommand(cmd) {
        if (!bankSessionId) return;
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: bankSessionId, data: cmd + '\n' })
        });
        document.getElementById('bank-amount-input').value = '';
    }

    function handleBancoData(text) {
        appendLog(`[Banco] ${text}`);
        const loginPanel = document.getElementById('bank-login-panel');
        const dashboardPanel = document.getElementById('bank-dashboard-panel');
        const balanceDisplay = document.getElementById('bank-balance-display');
        const activeAccLabel = document.getElementById('bank-active-acc-label');

        const lines = text.split('\n');
        lines.forEach(line => {
            line = line.trim();
            if (!line) return;

            if (line.includes('DIGITE O NUMERO DA CONTA:')) {
                fetch('/api/socket/send', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sessionId: bankSessionId, data: bankAccountNum + '\n' })
                });
            } else if (line.includes('MENU:')) {
                if (loginPanel && loginPanel.style.display !== 'none') {
                    loginPanel.style.display = 'none';
                    dashboardPanel.style.display = 'block';
                    activeAccLabel.textContent = `Conta #${bankAccountNum}`;
                    sendBankCommand('1');
                }
            } else if (line.includes('SALDO ATUAL:')) {
                const bal = line.replace('SALDO ATUAL:', '').trim();
                balanceDisplay.textContent = `R$ ${parseFloat(bal).toFixed(2)}`;
            } else if (line.includes('SUCESSO:')) {
                appendLog(`[Banco Sucesso] ${line}`, 'system');
                sendBankCommand('1');
            } else if (line.startsWith('ERRO:')) {
                appendLog(`[Banco Erro] ${line}`, 'stderr');
                if (line.includes('inexistente')) {
                    alert('Conta inexistente! Tente as contas iniciais "123" ou "456".');
                    disconnectBank();
                }
            }
        });
    }

    async function disconnectBank() {
        if (!bankSessionId) return;
        await fetch('/api/socket/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: bankSessionId, data: '4\n', closeWrite: true })
        });
        handleBancoClose();
    }

    function handleBancoClose() {
        const loginPanel = document.getElementById('bank-login-panel');
        const dashboardPanel = document.getElementById('bank-dashboard-panel');
        if (loginPanel) {
            loginPanel.style.display = 'flex';
            dashboardPanel.style.display = 'none';
        }
        bankSessionId = null;
        bankAccountNum = null;
    }

    // --- LOJAS HANDLERS ---
    async function triggerStoresClients() {
        appendLog(`[Lojas] Conectando 4 filiais simuladoras ao Sistema Central...`, 'system');
        
        for (let i = 0; i < 4; i++) {
            const card = document.getElementById(`filial-card-${i}`);
            if (card) card.className = 'filial-card-node';
            const val = document.getElementById(`filial-val-${i}`);
            if (val) val.textContent = 'R$ 0,00';
        }

        document.getElementById('central-total-sales').textContent = 'Calculando...';

        for (let i = 0; i < 4; i++) {
            fetch('/api/socket/run-client', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    clientClass: 'lojas.FilialSimulador',
                    args: [`Filial_${i}`]
                })
            }).catch(e => console.error(e));
        }
    }

    function parseLojasLine(line) {
        if (line.includes('Recebendo dados da Filial:')) {
            const filialId = line.split(':')[1].trim();
            const index = filialId.split('_')[1];
            const card = document.getElementById(`filial-card-${index}`);
            if (card) {
                card.className = 'filial-card-node sending';
                card.querySelector('.filial-val').textContent = 'Transmitindo...';
            }
        }
        else if (line.includes('Finalizado Filial')) {
            const match = line.match(/Finalizado Filial\s+Filial_(\d+)/);
            if (match) {
                const idx = match[1];
                const card = document.getElementById(`filial-card-${idx}`);
                if (card) {
                    card.className = 'filial-card-node';
                    card.querySelector('.filial-val').textContent = 'Finalizado';
                }
            }
        }
        else if (line.includes('TOTAL ACUMULADO NO SISTEMA CENTRAL:')) {
            const total = line.split('R$')[1].trim();
            const centralTotal = document.getElementById('central-total-sales');
            if (centralTotal) {
                centralTotal.textContent = `R$ ${parseFloat(total).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`;
            }
        }
    }

    // --- ARQUIVOS HANDLERS ---
    function renderServerFiles() {
        const container = document.getElementById('server-files-list');
        if (!container) return;
        if (uploadedFiles.length === 0) {
            container.innerHTML = '<div style="font-size: 0.75rem; color: var(--text-muted); text-align: center; margin-top: 20px;">Nenhum arquivo no servidor.</div>';
            return;
        }
        container.innerHTML = uploadedFiles.map(file => `
            <div class="file-item-row">
                <span class="file-name-label">${file}</span>
                <button class="btn btn-mini btn-secondary btn-download-file" id="btn-download-${file.replace('.', '_')}" data-name="${file}">Download</button>
            </div>
        `).join('');

        uploadedFiles.forEach(file => {
            document.getElementById(`btn-download-${file.replace('.', '_')}`).addEventListener('click', () => {
                downloadFile(file);
            });
        });
    }

    async function uploadFile(filename, content) {
        appendLog(`[Arquivos] Fazendo upload de ${filename}...`, 'system');
        const res = await fetch('/api/socket/file-upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename, content })
        });
        const data = await res.json();
        if (data.success) {
            appendLog(`[Arquivos] Sucesso: ${data.message}`);
            if (!uploadedFiles.includes(filename)) {
                uploadedFiles.push(filename);
            }
            renderServerFiles();
        } else {
            appendLog(`[Arquivos Erro] ${data.error}`, 'stderr');
        }
    }

    async function downloadFile(filename) {
        appendLog(`[Arquivos] Solicitando download de ${filename}...`, 'system');
        const res = await fetch('/api/socket/file-download', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename })
        });
        const data = await res.json();
        if (data.success) {
            appendLog(`[Arquivos] ${filename} baixado com sucesso.`);
            const card = document.getElementById('file-content-preview-card');
            if (card) {
                card.style.display = 'block';
                document.getElementById('downloaded-filename').textContent = filename;
                document.getElementById('downloaded-file-content').textContent = data.content;
            }
        } else {
            appendLog(`[Arquivos Erro] ${data.error}`, 'stderr');
        }
    }

    // --- CHAT MULTICAST HANDLERS ---
    async function joinChat() {
        const input = document.getElementById('chat-nickname-input');
        const nick = input.value.trim();
        if (!nick) return;

        activeChatNickname = nick;
        appendLog(`[Chat] Entrando no grupo multicast UTFPR com nick "${nick}"...`, 'system');
        
        const res = await fetch('/api/socket/chat-join', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nickname: nick })
        });
        const data = await res.json();
        if (data.success) {
            document.getElementById('chat-join-panel').style.display = 'none';
            document.getElementById('chat-active-panel').style.display = 'flex';
            appendLog(`[Chat] Conectado e escutando canal multicast!`, 'system');
            
            handleChatMessage(`SISTEMA: ${nick} entrou no chat.`);
        } else {
            appendLog(`[Chat Erro] ${data.error}`, 'stderr');
        }
    }

    async function sendChatMessage() {
        const input = document.getElementById('chat-message-input');
        const msg = input.value.trim();
        if (!msg) return;

        await fetch('/api/socket/chat-send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: msg })
        });
        input.value = '';
    }

    function handleChatMessage(text) {
        const container = document.getElementById('chat-messages');
        if (!container) return;

        const bubble = document.createElement('div');
        let isMine = false;
        let displayNick = 'Sistema';
        let displayMsg = text;

        if (text.startsWith('SISTEMA:')) {
            bubble.className = 'chat-bubble other';
            bubble.style.borderLeft = '3px solid var(--accent-info)';
            displayNick = 'Sistema';
            displayMsg = text.replace('SISTEMA:', '').trim();
        } else {
            const separatorIdx = text.indexOf(':');
            if (separatorIdx !== -1) {
                const nick = text.substring(0, separatorIdx).trim();
                const msg = text.substring(separatorIdx + 1).trim();
                
                displayNick = nick;
                displayMsg = msg;
                if (nick === activeChatNickname) {
                    isMine = true;
                }
            }
            bubble.className = isMine ? 'chat-bubble mine' : 'chat-bubble other';
        }

        bubble.innerHTML = `
            <div class="chat-bubble-nickname">${displayNick}</div>
            <div>${displayMsg}</div>
        `;

        container.appendChild(bubble);
        container.scrollTop = container.scrollHeight;
    }

    async function leaveChat() {
        appendLog(`[Chat] Saindo do canal multicast...`, 'system');
        await fetch('/api/socket/chat-leave', { method: 'POST' }).catch(() => {});
        
        document.getElementById('chat-join-panel').style.display = 'flex';
        document.getElementById('chat-active-panel').style.display = 'none';
        document.getElementById('chat-messages').innerHTML = '';
        
        activeChatNickname = '';
    }

    // --- ORIGINAL UTILITIES ---
    function updateBadge(state, text) {
        statusBadge.className = `badge ${state}`;
        statusBadge.textContent = text;
    }

    function appendLog(text, className = '') {
        const div = document.createElement('div');
        div.className = `line ${className}`;
        
        const cleanText = text.replace(/[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]/g, '');
        
        div.textContent = cleanText;
        consoleOutput.appendChild(div);
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }

    btnClearConsole.addEventListener('click', () => {
        consoleOutput.innerHTML = '';
    });

    document.addEventListener('click', (e) => {
        if (e.target && e.target.id === 'btn-run-rmi-client') {
            fetch('/api/rmi/run-client', { method: 'POST' })
                .catch(err => appendLog(`[Erro] ${err.message}`, 'stderr'));
        }
    });
});
