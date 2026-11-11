# Changelog

As alterações do [brModelo NG](https://github.com/kpagnussat/brModeloNG) são
registradas neste arquivo, no formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Esta seção reúne as mudanças do fork em relação ao brModelo oficial 3.3.2.
O histórico anterior pertence ao
[projeto original](https://github.com/chcandido/brModelo/commits/master/).

## [1.0.1] - 2026-11-10

Versão de segurança. O formato `.brM3` não muda: os arquivos continuam abrindo
no brModelo oficial, e vice-versa.

### Security

- **Leitura fora da janela:** `.brM3` e `.brMj` abertos pelo menu, pelos
  recentes, pela linha de comando ou pelo gerenciador de arquivos são
  decodificados num processo Java separado e sem tela (768 MB de memória,
  30 segundos de prazo), que devolve o diagrama em JSON, validado de novo pela
  janela; a interface deixou de desserializar arquivos recebidos. Um arquivo
  feito para esgotar a memória ou o processador travava o programa; agora
  encerra só esse processo e é recusado. No Linux, o processo roda no
  bubblewrap, sem rede, pasta pessoal, tela ou barramento da sessão; no Flatpak,
  com `flatpak-spawn --sandbox` e ambiente vazio, encerrado pelo próprio
  programa no prazo. Sem isolamento disponível, roda como processo comum e o
  motivo fica no log.
- **Abertura sem travar:** a janela continua respondendo enquanto o arquivo é
  lido. Se a leitura passa de meio segundo, aparece **Abrindo diagrama**, com
  **Cancelar**, que encerra a leitura e devolve a janela na hora. Arquivos
  passados pela linha de comando continuam abrindo na ordem.
- **Limites de entrada:** diagramas binários, partes e recuperação até 64 MiB,
  JSON até 128 MiB, XML e texto colado até 16 MiB, conferidos antes de ler; o
  leitor recusa vetores e coleções declarados maiores que a própria entrada.
  Imagens até 64 megapixels, conferidas pelo cabeçalho; setas desenhadas sem uma
  imagem do tamanho da forma; exportar, copiar e imprimir recusam diagramas
  acima do teto; potências nas expressões limitadas antes do cálculo.
- **XML:** lido sem DTD e sem entidades externas; antes, um XML preparado podia
  ler arquivos do computador e enviá-los pela rede.
- **Rede:** textos do diagrama nunca são interpretados como HTML, que fazia o
  Swing buscar imagens na internet ao abrir um arquivo; o processo recusa
  endereços `http`, `https` e `ftp`; o visualizador de SQL escapa os nomes; os
  links do Sobre aceitam só `https`.
- **Segunda camada de rede nos pacotes:** `.deb`, `.rpm` e `.tar.gz` iniciam o
  programa no bubblewrap, numa rede só com a interface local, e abrem links e a
  ajuda pelo portal OpenURI ou pelo gerenciador de serviços do usuário, para o
  navegador ter rede; sem bubblewrap, abrem normalmente com registro no log. O
  `.msi` acrescenta regras de saída bloqueada no Firewall do Windows para o
  programa e o Java incluído, removidas ao desinstalar e restauradas se um
  reparo ou uma desinstalação falhar.
- **Flatpak sem acesso a pastas:** a permissão para a pasta Documentos saiu;
  cada arquivo chega pelo seletor ou pelo gerenciador de arquivos, via portal de
  documentos, e salvar sobre ele continua atômico.
- **Gravação atômica:** diagramas, recuperação, partes, configurações, SQL,
  texto e imagens exportadas são gravados num temporário na mesma pasta,
  sincronizados e só então trocados pelo original; uma falha no meio, como disco
  cheio, mantém o arquivo anterior.
- **SQL gerado:** nomes que poderiam carregar comandos saem entre aspas (nomes
  comuns não mudam), `ON DELETE`/`ON UPDATE` e o separador aceitam só valores do
  SQL, e `*/` não fecha mais o comentário do cabeçalho.
- **JSON:** listas e mapas não são mais aceitos como chave de mapa.
- **Sessão X11:** ao iniciar numa sessão X11, o programa avisa uma vez que nela
  qualquer janela pode enviar teclas a outras, inclusive a um terminal, e
  recomenda a sessão Wayland para abrir arquivos de outras pessoas. É a lacuna
  que segue aberta: fechá-la depende de um Java que desenhe direto no Wayland.
- **Distribuição:** dependências verificadas na base OSV a cada mudança, toda
  semana e antes de cada release; releases com `SHA256SUMS` e atestado de
  procedência para cada arquivo (`gh attestation verify`).

### Added

- **Ferramenta fixa na paleta** (issue #5 do brModelo original): para criar
  vários objetos seguidos, como os atributos de uma entidade, era preciso
  escolher a ferramenta de novo a cada um. Agora um clique duplo na ferramenta
  a mantém ativa até **Esc**, um clique na seta ou a escolha de outra
  ferramenta. O atalho antigo, segurar **Ctrl** ao clicar na folha, continua
  valendo e passou a constar na ajuda.
- **Tipo UUID** (issue #11 do brModelo original): a lista de tipos de campo do
  modelo lógico, em **Editar campos** e **Editar tipos**, passou a sugerir
  `UUID`, também nos diagramas salvos antes dele. O arquivo continua abrindo no
  brModelo oficial.

### Changed

- **Flatpak:** recentes guardados em `~/Documentos` pela versão 1.0.0 deixam de
  abrir pela lista; abra cada um uma vez por **Arquivo → Abrir**, e ele volta a
  ficar nos recentes.
- **Abertura:** a leitura num processo separado acrescenta cerca de 0,2 a 0,4 s
  a um diagrama típico e alguns segundos aos muito grandes (2.000 itens
  interligados: 2,4 s, contra 0,16 s).
- **Fonte da interface:** Adwaita Sans incluída (SIL OFL 1.1) e usada no
  Windows e no macOS. O Linux segue a fonte da área de trabalho e usa a cópia
  incluída quando o Java não a encontra, como no Flatpak no Silverblue e no
  Kinoite, onde a interface aparecia numa fonte fina de reserva.
- **Pacotes Linux:** `.deb` e `.rpm` dependem de `bubblewrap`; `brmodelo-ng` é
  um script que inicia `brmodelo-ng-app`, e `BRMODELO_ISOLAMENTO=desligado`
  dispensa a rede isolada.
- **Desenvolvimento:** a CI gera o `.msi` a cada mudança e roda os testes de
  isolamento sem pulá-los; o projeto tem uma política de segurança
  ([SECURITY.md](SECURITY.md)) e o README, uma seção que descreve cada camada.
- **Orientação para quem troca diagramas:** o README e a ajuda (página
  **Abrir arquivos de outras pessoas**) explicam a professores e alunos o que o
  NG faz ao abrir um arquivo recebido e os cuidados que nenhuma proteção
  substitui: conferir a extensão, não abrir no oficial um arquivo recusado e
  ler o SQL gerado antes de executá-lo.

### Fixed

- Ajuda: o tópico "A ajuda não abriu" citava um servidor local do Flatpak que
  não existe mais.
- **Conversão para o lógico, atributo multivalorado** (issue #13 do brModelo
  original): a tabela criada para o atributo recebia uma chave própria, e a
  entidade passava a apontar para ela, invertendo a ligação. Agora a tabela do
  atributo recebe a chave da entidade como chave estrangeira e tem como chave
  primária essa chave mais o valor, uma linha por valor. Um máximo pequeno,
  como (1,3), não repete mais a coluna na tabela.
- **Especialização exclusiva ou não exclusiva** (issue #33 do brModelo
  original): o Inspector só mostrava a indicação, e as ferramentas da paleta
  criavam sempre um triângulo único, ou seja, exclusiva. Agora **Esp.
  exclusiva** e **Esp. não exclusiva** podem ser marcadas: o programa junta as
  entidades especializadas num triângulo ou desenha um triângulo para cada uma,
  mantendo total ou parcial, e um **Desfazer** volta ao desenho anterior. A
  ajuda descrevia a ferramenta **Nova Especialização (duas entidades)** como
  não exclusiva; ela cria um triângulo com duas entidades.
- **Nome digitado no Inspector** (issue #7 do brModelo original): o texto só
  valia com Enter, e clicar no desenho, em outra linha ou salvar descartava o
  que tinha sido digitado. Agora ele vale também ao sair do campo, ao clicar no
  desenho ou na lista de objetos e antes de qualquer comando do menu, como
  **Salvar**. Esc continua desfazendo a digitação.
- **Erro de imagem no log** (issue #31 do brModelo original): ao clicar num
  espaço vazio do diagrama, o retângulo de seleção aparecia na lista de
  objetos, e o log registrava `ERROR_GET_RESOURCE_IMG` por falta de ícone.
  Agora ele fica fora da lista. As restrições do modelo lógico (chave
  primária, chave estrangeira e única) também ganharam o ícone na lista, que
  nunca aparecia e gerava o mesmo erro.
- **Nome de atributo quebrado na imagem exportada** (issue #25 do brModelo
  original): com tamanho automático, o atributo media o nome na escala da tela
  (zoom, ou telas em 125% e 150%), e na exportação, impressão ou outro zoom as
  últimas letras podiam cair para uma segunda linha. Agora o nome de um
  atributo com tamanho automático nunca quebra linha. A largura salva no
  arquivo não muda.
- **Atalhos no macOS** (issue #27 do brModelo original): todos os atalhos
  pediam Ctrl, onde o macOS usa Command. Agora os menus, os diálogos e o
  desenho usam **Command (⌘)** no macOS: segurar Command ao clicar soma à
  seleção e mantém a ferramenta, e Command com a roda do mouse muda o zoom.
  Ctrl+Tab continua com Ctrl, porque Command+Tab troca de programa. Linux e
  Windows não mudam.

## [1.0.0] - 2026-10-09

Primeira versão do **brModelo NG**.

### Added

- **Interface:** 18 temas FlatLaf (7 claros e 11 escuros) no diálogo
  **Editar → Aparência…**, com prévias; ao seguir o sistema, um tema preferido
  para cada modo e troca automática quando o desktop muda; opção
  `-Dbrmodelo.tema` com `claro`, `escuro`, `sistema` ou o identificador de um
  tema; ícones SVG para ações, paleta, abas e árvores, com cores adaptadas ao tema.
- **Edição:** encaixe na grade ao mover, criar e redimensionar (ligado por
  padrão, Alt solta; setas movem um passo da grade); **Organizar ligações**
  escolhe o lado de cada linha, ordena os pontos sem cruzamentos e os distribui
  por igual, em um único passo de desfazer.
- **Ajuda:** tópicos em Markdown (`docs/ajuda/`) gerados como site offline,
  incluído no jar e nos pacotes e aberto no navegador por **F1**; no Flatpak,
  uma página HTML única aberta pelo portal do sistema, sem permissão de rede.
- **Status:** indicador de mensagens e erros não lidos com contador, símbolo e
  acesso ao log; mensagens temporárias com expiração em cinco segundos.
- **Arquivos:** formato JSON opcional `.brMj`, determinístico e versionado,
  com abertura, salvamento e conversão de volta para `.brM3`.
- **Integração:** seletor de arquivos do sistema em cada plataforma (XDG Desktop
  Portal no Linux/BSD, diálogo do Windows e painel do macOS), com a lista de tipos
  de arquivo, e alternativas AWT e Swing; memória da última pasta e confirmação
  de substituição para o caminho final.
- **Distribuição:** identidade brModelo NG, ícone próprio, abertura de arquivos
  por argumentos e associações `.brM3`/`.brMj` (*Diagrama brModelo* e *Diagrama
  JSON brModelo*); tarefas para app-image, DEB, RPM,
  MSI, DMG e bundle Flatpak com ambiente Java incluído; versões portáteis
  para Linux (`.tar.gz`) e Windows (`.zip`), que dispensam instalação.
- **Desenvolvimento:** build Gradle com Java 21, jar portátil com dependências,
  catálogo de versões e verificação de dependências por checksum; CI para Linux,
  Windows e macOS e geração de releases em rascunho a partir de tags.
- **Verificação:** testes de persistência, segurança, interface e utilitários;
  seis diagramas de exemplo inventados com dumps canônicos; teste da forma
  serializada e teste de compatibilidade bidirecional com o jar oficial isolado,
  inclusive após conversão por JSON; tarefas de capturas e auditoria de arquivos.

### Changed

- **Versão:** identidade NG 1.0.0 derivada do Gradle no Sobre e nos pacotes;
  formato de diagramas preservado em 3.2.0. Releases acessíveis pelo menu Ajuda,
  sem consultas automáticas ao site do brModelo oficial.
- **Dicas:** tooltips descritivos nas propriedades, controles, editores e impressão;
  valores cortados aparecem por inteiro e as dicas informam a ação disponível.

- **Layout:** barras, Inspector, editores, conversor, ajuda e diálogos de impressão
  dimensionados pelo conteúdo e pela fonte; coluna esquerda ajustada às abas,
  abas do Inspector em uma linha rolável e cabeçalho vazio da paleta oculto.
- **Aparência:** Inspector como grade de propriedades, cabeçalhos com chevrons,
  ações compactas, dicas integradas com placeholder, paleta de ferramentas
  que se ajustam à altura disponível em uma coluna de largura fixa e mesa temática
  com a folha delimitada por uma borda fina. Barras alinhadas, grupos com
  espaçamento, abas com destaque e fechar ao passar o mouse, rolagem fina,
  cantos e padding consistentes nos diálogos. Cores e superfícies acompanham a
  troca ao vivo; a folha, o conteúdo, a impressão e a exportação são preservados.
- **Janelas:** moldura FlatLaf no Windows e moldura do sistema no Linux e no
  macOS (`-Dbrmodelo.decoracoes=modernas|nativas` muda o padrão); no macOS, os
  menus ficam na barra do sistema.
- **Diagrama:** ícones de ação da seleção e chaves das tabelas vetoriais, nítidos
  em qualquer zoom, em HiDPI e na exportação; a faixa de ícones vai para um lado
  livre da forma. Inspector com aparência própria para cada tipo de propriedade;
  editores de IR em layout mestre/detalhe.
- **Desktop:** configurações, recuperação e modelos reutilizáveis em pastas
  próprias do NG: XDG no Linux/Unix, Application Support no macOS e LocalAppData
  no Windows; cópia inicial dos arquivos legados da pasta de execução.
- **Navegação:** roda do mouse rola verticalmente por padrão; Shift permite
  rolagem horizontal.
- **Desempenho e estrutura:** diálogo e descoberta de impressoras adiados até o
  primeiro uso (janela visível em 0,9 s, contra 3,1 s no oficial 3.3.2); responsabilidades de classes
  grandes extraídas para auxiliares, preservando a forma serializada. A tela
  pinta só as linhas visíveis da grade, os ícones SVG da interface são
  renderizados uma vez por escala e tema, e no Linux o Java2D usa padrões que
  evitam leituras de volta do servidor X ao redimensionar.
- **Documentação:** README em português com resumo em inglês, capturas dos
  exemplos versionados, instruções de uso, compilação e formatos, e as outras
  versões do brModelo.

### Fixed

- Prévia de impressão e pintura externa alterando dimensões, posições ou cache
  de texto do diagrama; posicionamento inicial do texto nas formas retangulares.
- Salvamento de texto acrescentando conteúdo ao arquivo existente em vez de
  substituí-lo após a confirmação.
- Salvamento automático concorrendo com a edição do diagrama; gravações agora
  usam snapshots capturados na thread da interface e uma fila de escrita, com
  espera pela conclusão ao sair.
- Atualizações de status fora da thread da interface e criação de threads de
  temporizador que permaneciam ativas.
- Campos e botões cortados com fontes maiores, colunas de PK/FK/Unique
  desalinhadas, barras sobrepostas na impressão e coluna do Inspector estreita.
- Cores e separadores incompatíveis com temas escuros; barras de ferramentas
  que podiam se soltar em janelas flutuantes; grafia de “Mostrar”.
- Diagramas grandes e muito interligados (cerca de 2.000 itens) estouravam a
  pilha ao abrir, salvar e desfazer, como no oficial; a serialização roda numa
  thread com pilha maior, sem mudar o formato.
- Visualizador de código cortando a última letra de uma palavra-chave no início
  da linha.
- Seleção do visual GTK em desktops Linux além do GNOME, ao optar pelo visual
  do sistema, com alternativa quando GTK não está disponível.

### Security

- Leitura de diagramas binários, streams internos, modelos reutilizáveis,
  recuperação e área de transferência com validação de classes antes da
  resolução e rejeição de proxies; entradas inválidas de recuperação são
  ignoradas individualmente.
- JSON usa a mesma regra de classes permitidas e valida campos, tipos,
  referências, números e versão antes de construir objetos do modelo.

### Removed

- Código morto e trechos comentados sem uso, sem alterar campos persistidos.
- Visualizador de ajuda legado e o arquivo `Ajuda.brMh`, substituídos pelo site
  gerado de `docs/ajuda/`.
- Build Ant/NetBeans e jars de dependências versionados; o projeto usa o Gradle
  Wrapper e Maven Central, preservando os arquivos `.form` para edição visual.

[1.0.1]: https://github.com/kpagnussat/brModeloNG/releases/tag/v1.0.1
[1.0.0]: https://github.com/kpagnussat/brModeloNG/releases/tag/v1.0.0
