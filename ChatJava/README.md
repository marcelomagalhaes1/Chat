# Sistema de Chat Distribuído com Sockets em Java

Este projeto implementa um sistema de chat distribuído em Java utilizando Sockets, com uma arquitetura Cliente-Servidor. Ele demonstra conceitos fundamentais de sistemas distribuídos, como comunicação síncrona/assíncrona e coordenação através de um líder.

## Estrutura dos Arquivos
- **'Servidor.java'**: O hub central que aceita conexões de clientes, gerencia a lista de participantes, retransmite mensagens e aplica as regras de moderação.

- **'ClientHandler.java'**: Uma classe Runnable no lado do servidor, onde cada instância lida com a comunicação de um único cliente em uma thread separada.

- **'ClienteGUI.java'**: A aplicação cliente com interface gráfica (GUI) que se conecta ao servidor, permitindo ao usuário interagir no chat de forma visual.

Nosso projeto tem exatamente essas duas partes: um programa Servidor.java e um programa ClienteGUI.java (GUI significa Graphical User Interface, ou Interface Gráfica do Usuário).

## Como Rodar a Aplicação
Para ver a mágica acontecer, você precisa primeiro compilar e depois executar os programas. É como montar as peças de um brinquedo e depois ligá-lo.

### Pré-requisitos
Você precisa ter o JDK (Java Development Kit) instalado no seu computador. É ele que nos dá as ferramentas para compilar e rodar código Java.

1. Compilar o Código
Abra o terminal ou prompt de comando do seu sistema.

Navegue até a pasta onde estão os arquivos .java (Servidor.java, ClienteGUI.java, ClientHandler.java).

Digite o seguinte comando e pressione Enter:

javac \*.java

Este comando diz ao Java para compilar todos os arquivos que terminam com .java naquela pasta. Se tudo der certo, ele vai gerar os arquivos .class, que são a versão do nosso código que o computador entende.

2. Iniciar o Servidor
No mesmo terminal, com o servidor esperando para começar a festa, digite:

java Servidor

Você verá uma mensagem como "Servidor de Chat iniciado na porta 12345...". Isso significa que o organizador está pronto e esperando os convidados chegarem.

Deixe essa janela do terminal aberta! Se você fechá-la, a festa acaba.

3. Iniciar os Clientes
Abra um novo terminal (não feche o do servidor!).

Digite o seguinte comando para iniciar o primeiro cliente:

java ClienteGUI

Uma série de janelas pop-up vai aparecer pedindo:

Endereço IP do servidor: Se você está rodando tudo no mesmo computador, pode digitar localhost ou 127.0.0.1.

Porta do servidor: Nosso servidor usa a porta 12345.

Seu nome de usuário: Escolha um apelido!

Modo de comunicação: Síncrono ou Assíncrono.

Depois de preencher tudo, a janela do chat aparecerá. Para ver a conversa, abra outro terminal e rode java ClienteGUI de novo para conectar um segundo cliente!

## O Cérebro da Operação: O Servidor (Servidor.java)
Vamos espiar por trás das cortinas e entender o que o nosso "Organizador da Festa", o Servidor.java, realmente faz. Pense no servidor como o gerente de um prédio de apartamentos.

Principais Responsabilidades do Gerente (Servidor.java)
Abrir a Portaria (startServer): O código ServerSocket serverSocket = new ServerSocket(PORT) é como o gerente abrindo a portaria do prédio em um endereço específico (a porta 12345). Ele fica lá, esperando alguém tocar o interfone.

Atender o Interfone (serverSocket.accept()): O programa fica parado nessa linha, esperando. Assim que um novo cliente tenta se conectar, o método accept() atende e cria uma conexão direta e privada com ele.

## Contratar um "Porteiro Pessoal" (ClientHandler): 
Para cada novo cliente, o gerente contrata um ClientHandler. O código new Thread(clientHandler).start() significa que cada porteiro trabalha de forma independente e simultânea.

Manter uma Lista de Contatos (clients): O servidor tem uma lista (clients) com o contato de cada "porteiro pessoal" que está trabalhando. Ele adiciona (addClient) e remove (removeClient) conforme os clientes entram e saem.

Fazer Anúncios Gerais (broadcastMessage): Quando um cliente envia uma mensagem, o gerente (Servidor) a distribui para todos os outros, exceto para o remetente original.

Definir o "Síndico" (leader): O primeiro cliente a se conectar se torna o "síndico" (leader) e tem poderes especiais, como o de silenciar (mute) outros usuários através de comandos.

O Assistente Pessoal: ClientHandler.java
Agora vamos conhecer o "porteiro pessoal" que o servidor contrata para cada cliente. Ele é o intermediário direto entre um único cliente e o gerente (servidor).

O Dia de Trabalho de um ClientHandler
Começando o Turno (run): O método run() representa o turno inteiro do ClientHandler. Ele fica ativo durante toda a conexão do cliente.

Ouvindo o Cliente (in.readLine()): A tarefa mais importante é ouvir. O ClientHandler fica "travado" nesta linha, esperando o seu cliente dizer alguma coisa.

Processando o que Foi Dito: Quando uma mensagem chega, ele decide o que fazer:

É um aviso de "digitando"? Ele avisa o gerente para notificar a todos.

É um comando especial (começa com /)? Ele verifica se o cliente é o síndico antes de executar.

É uma mensagem normal? Ele a entrega para o gerente distribuir.

Fim do Turno (bloco finally): Se a conexão cai, ele avisa o gerente (server.removeClient(this)) e encerra tudo de forma limpa.

## A Janela do Chat: A Interface do Cliente (ClienteGUI.java)
Finalmente, chegamos à parte que o usuário vê! Pense na interface como uma folha de papel mágica com uma caneta.

Montando a Nossa Janela
Coletando Informações (collectUserInfo): Antes de tudo, o programa usa JOptionPane para abrir janelas que pedem o IP, a porta e seu nome de usuário.

Estabelecendo a Conexão (connectToServer): Com as informações, ele "liga" para o servidor. Se a conexão falhar, exibe um erro.

Desenhando a Tela (initUI): Esta parte desenha os componentes visuais:

JTextArea (chatArea): Onde o histórico da conversa aparece.

JTextField (messageField): Onde você escreve sua mensagem.

JButton (sendButton): O botão "Enviar".

JLabel (typingStatusLabel): O espaço que mostra quem está digitando.

A Mágica Acontecendo: A Lógica do Cliente (ClienteGUI.java)
Agora que a janela está montada, como ela funciona?

O Truque do "Digitando..."
Um "espião" (DocumentListener) observa o campo de texto. Quando você começa a digitar, ele envia TYPING_START para o servidor e ativa um cronômetro (Timer) de 1.5 segundos. Cada tecla pressionada reinicia o cronômetro. Se você parar de digitar, o cronômetro dispara e envia TYPING_STOP.

Enviando uma Mensagem (sendMessage)
Quando você aperta Enter ou clica em "Enviar", o programa envia sua mensagem para o servidor, a exibe na sua própria tela e limpa o campo de texto.

Comunicação Síncrona vs. Assíncrona
Assíncrona (Padrão): Você pode enviar e receber mensagens a qualquer momento, de forma fluida.

Síncrona (Especial): Após enviar uma mensagem, a interface bloqueia a digitação até receber uma confirmação ("ACK") do servidor, garantindo que a mensagem foi entregue antes de você enviar a próxima.

Ouvindo em Segundo Plano (SwingWorker)
O programa contrata um "assistente secreto" (SwingWorker) que trabalha em segundo plano. Sua única tarefa é ficar ouvindo a conexão com o servidor. Assim que uma mensagem chega, ele a entrega para a interface gráfica exibir, garantindo que a janela nunca "trave".

## Conclusão
E é isso! Vimos como o Servidor age como um gerente central, o ClientHandler como um porteiro pessoal, e o ClienteGUI como a janela de interação do usuário. Juntos, eles criam um sistema de chat completo e funcional.
