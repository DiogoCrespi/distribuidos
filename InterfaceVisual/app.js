document.addEventListener('DOMContentLoaded', () => {
    // --- ESTADO GLOBAL ---
    let currentExerciseId = 'bar';
    let sseSource = null;
    let isRunning = false;
    let bufferItems = [];

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
        }
    };

    // --- CONFIGURAÇÃO INICIAL ---
    setupSSE();
    resetAllUIs();

    // --- NAVEGAÇÃO ENTRE ABAS ---
    menuButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-id');
            if (id === currentExerciseId) return;

            // Se estiver rodando algo, para
            if (isRunning) {
                stopCurrentExercise();
            }

            // Altera botão ativo na sidebar
            document.querySelector('.menu-btn.active').classList.remove('active');
            btn.classList.add('active');

            // Altera visualizadores de painéis
            document.querySelector('.sim-panel.active').classList.remove('active');
            document.getElementById(EXERCISES[id].panelId).classList.add('active');

            currentExerciseId = id;
            currentTitle.textContent = EXERCISES[id].title;
            currentDesc.textContent = EXERCISES[id].desc;

            // Reseta telas
            resetAllUIs();
            appendLog(`[Interface] Alternado para ${EXERCISES[id].title}`, 'system');
        });
    });

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
    function parseFilosofosLine(line) {
        // Meditando
        if (line.match(/Filosofo\s+(\d+):\s+Meditando\.\.\./)) {
            const match = line.match(/Filosofo\s+(\d+):\s+Meditando\.\.\./);
            const id = parseInt(match[1]);
            updatePhilosopherCard(id, 'meditating', 'Meditando', 'Sem hashis');
            const n = document.querySelectorAll('#philosophers-ring .phil-circle-node').length || 5;
            // Libera hashis do filósofo
            updateHashiUI(id, 'Livre');
            updateHashiUI((id + 1) % n, 'Livre');
        }
        // Tentando comer (pegou esquerdo)
        else if (line.match(/Filosofo\s+(\d+):\s+Pegou hachi esquerdo\.\s*Tentando o direito\.\.\./)) {
            const match = line.match(/Filosofo\s+(\d+):\s+Pegou hachi esquerdo\.\s*Tentando o direito\.\.\./);
            const id = parseInt(match[1]);
            updatePhilosopherCard(id, 'trying', 'Aguardando', `Pego: H${id}`);
            updateHashiUI(id, `Em uso (F${id})`);
        }
        // Comendo
        else if (line.match(/Filosofo\s+(\d+):\s+Comendo arroz\.\.\./)) {
            const match = line.match(/Filosofo\s+(\d+):\s+Comendo arroz\.\.\./);
            const id = parseInt(match[1]);
            const n = document.querySelectorAll('#philosophers-ring .phil-circle-node').length || 5;
            updatePhilosopherCard(id, 'eating', 'Comendo', `Pegos: H${id}, H${(id+1)%n}`);
            updateHashiUI(id, `Em uso (F${id})`);
            updateHashiUI((id + 1) % n, `Em uso (F${id})`);
        }
    }

    function updatePhilosopherCard(id, cssClass, status, hashis) {
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

    function updateHashiUI(hashiId, statusText) {
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
    }

    function updateBadge(state, text) {
        statusBadge.className = `badge ${state}`;
        statusBadge.textContent = text;
    }

    function appendLog(text, className = '') {
        const div = document.createElement('div');
        div.className = `line ${className}`;
        
        // Limpa sequências ANSI de controle de terminal
        const cleanText = text.replace(/[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]/g, '');
        
        div.textContent = cleanText;
        consoleOutput.appendChild(div);
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }

    btnClearConsole.addEventListener('click', () => {
        consoleOutput.innerHTML = '';
    });
});
