 
 
Exercícios 
 
01)	Atendimento no Bar: O funcionamento do atendimento em um bar é baseado em um bartender que recebe o pedido dos garçons, que por sua vez recebe o pedido dos clientes. Neste contexto, o bar possui um (01) bartender que fica esperando os garçons na copa, possui X garçons, sendo que cada garçom consegue atender a um número limitado (C) de clientes por vez. Cada garçom somente vai para a copa para realizar o pedido quando todos os C clientes que ele pode atender tiverem feito o pedido, ou não houver mais clientes a serem atendidos. Um garçom deve entregar os pedidos aos seus clientes, e depois está liberado a atender os clientes novamente. Após ter seu pedido atendido, um cliente pode fazer um novo pedido após consumir seu pedido (o que leva um tempo aleatório). Por definição, nem todos os clientes precisam fazer um pedido a cada rodada. Implemente uma solução que permita a passagem por parâmetro de: 
a.	A quantidade de clientes presentes no estabelecimento; 
b.	A quantidade de garçons que estão trabalhando; 
c.	A capacidade de atendimento dos garçons; e  
d.	O número de rodadas que serão liberadas no bar.  
Cada garçom e cada cliente devem ser representados por threads, estruturalmente definidos como os pseudocódigos que seguem (exemplos): 

thread cliente {
    while (!fechouBar){
        fazPedido();
        esperaPedido();
        recebePedido();
        consomePedido(); //tempo variável
    }
}
thread garçom {
    while (existemClientesNoBar){
        recebeMaximoPedidos();
        registraPedidos();
        entregaPedidos();
        rodada++; //serve como parâmetro para fechar o bar
    }
}

  
A ordem de chegada dos pedidos dos clientes na fila de pedidos de cada garçom deve ser respeitada. Sua solução não deve permitir que clientes furem essa fila. O garçom só pode ir para a copa quando tiver recebido seus C pedidos. O programa deve  mostrar a evolução, portanto planeje bem o que será apresentado. Deve ficar claro o que está acontecendo no bar a cada rodada: os pedidos dos clientes, os atendimentos pelos garçons, os deslocamentos para o pedido, a garantia de ordem de atendimento, etc. 
 
02)	Barbeiro Dorminhoco: Um barbeiro corta o cabelo de qualquer cliente. Se não há clientes, o barbeiro tira uma soneca. Há várias threads, uma para cada cliente. Um cliente aguarda pelo barbeiro se há ao menos uma cadeira vazia na barbearia, caso contrário, o cliente sai da barbearia imediatamente. Se há uma cadeira disponível, então o cliente senta. Se o barbeiro está dormindo, então o cliente acorda-o. Existem N cadeiras na barbearia. Faça um programa para a classe BarbeiroDorminhoco utlizando monitor. 
 
03)	O Jantar dos Filósofos: Neste problema, um grupo de cinco filósofos chineses alterna suas vidas entre meditar e comer. Ha uma mesa redonda com um lugar fixo para cada filósofo, com um prato, cinco palitos (hashis) compartilhados e um grande prato de arroz ao meio. Para comer, um filósofo fi precisa pegar o palito à sua direita (pi) e à sua esquerda (pi+1), um de cada vez. Como os palitos são compartilhados, dois filósofos vizinhos não podem comer ao mesmo tempo. Os filósofos não conversam entre si nem podem 
 A felicidade não se resume na ausência de Com a força da sua mente, seu instinto e, Meus filhos terão computadores, sim, mas antes terão livros. Sem livros, 
sem leitura, os nossos filhos serão incapazes de escrever - inclusive a sua  problemas, mas sim na sua capacidade de lidar  Éramos Felizes e nem Sabíamos também com sua experiência você pode  com eles. Bob Marley voar alto. própria história. 
 Einstein  Ayrton Senna Bill Gates 
    
 
observar os estados uns dos outros. O problema do jantar dos filósofos é representativo de uma grande classe de problemas de sincronização entre vários processos e vários recursos sem usar um coordenador central. Resolver o problema do jantar dos filósofos consiste em encontrar uma forma de coordenar suas ações de maneira que todos os filósofos consigam meditar e comer. 

“”Esta imagem é uma ilustração didática de um conceito clássico da ciência da computação chamado "O Jantar dos Filósofos" (neste caso, representado por pandas), usado para explicar problemas de sincronização e deadlock em sistemas operacionais.
Aqui está uma descrição detalhada para você copiar:
________________________________________
Descrição da Imagem: O Jantar dos Pandas
A imagem apresenta um diagrama esquemático de uma mesa redonda vista de cima, organizada da seguinte forma:
•	Centro da Mesa: Há uma grande tigela circular contendo arroz.
•	Pandas (\(f_{0}\) a \(f_{4}\)): Cinco cabeças de pandas estão posicionadas ao redor da mesa, representando os "filósofos". Eles estão rotulados de \(f_{0}\) (à direita) seguindo em sentido horário até \(f_{4}\) (no topo).
•	Pratos: À frente de cada panda, sobre a mesa azul clara, há um prato amarelo circular.
•	Talheres/Pausinhos (\(p_{0}\) a \(p_{4}\)): Entre cada prato, há uma linha preta vertical representando um talher (ou hachi). Eles estão rotulados de \(p_{0}\) a \(p_{4}\).
•	Dinâmica do Problema: Cada panda individual possui um talher à sua esquerda e outro à sua direita. Por exemplo, o panda \(f_{0}\) está entre os talheres \(p_{0}\) e \(p_{1}\). Para comer o arroz no centro, cada panda precisa segurar simultaneamente os dois talheres adjacentes ao seu prato.
””
  
 
04) Quanto aos clássicos problemas envolvendo threads: 
a.	Recrie o problema das Roletas para funcionar com qualquer quantidade de roletas 
b.	Simule diferentes ações em contas bancárias ao mesmo tempo (cada ação é uma thread) 
i.	Transferência, depósito, crédito de juros 
ii.	Saque, Depósito, crédito de juros, transferência 
c.	Crie um problema clássico de Produtor/Consumidor e resolva o mesmo utilizando semáforos e outro utilizando monitores 
 
 A felicidade não se resume na ausência de Com a força da sua mente, seu instinto e, Meus filhos terão computadores, sim, mas antes terão livros. Sem livros, 
sem leitura, os nossos filhos serão incapazes de escrever - inclusive a sua  problemas, mas sim na sua capacidade de lidar  Éramos Felizes e nem Sabíamos também com sua experiência você pode   com eles.Einstein 	 Bob Marley 	 voar alto.Ayrton Senna  Bill Gatesprópria história. 	 
    
 
