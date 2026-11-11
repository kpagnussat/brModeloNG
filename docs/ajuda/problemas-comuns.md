# Problemas comuns

## Meu arquivo abre no brModelo oficial?

Use **`.brM3`** para compartilhar com o oficial 3.3.2. O NG preserva o formato binário e os testes do projeto conferem leitura, round-trip e abertura dos fixtures no jar oficial. Isso não garante que um arquivo incompleto ou danificado possa ser recuperado.

O oficial **não lê `.brMj`**. Abra o JSON no NG e use **Arquivo → Salvar Como…**, escolhendo `.brM3`. Trocar apenas a extensão do nome não converte o conteúdo. Confira o resultado no aplicativo de destino antes da entrega.

## Onde está meu auto-salvamento?

O arquivo chama-se **`autosave.chc`**. No Linux, fica em `$XDG_STATE_HOME/brmodelo-ng/` ou, sem essa variável, `~/.local/state/brmodelo-ng/`. No Windows, fica em `%LOCALAPPDATA%/brModelo NG`; no macOS, em `~/Library/Application Support/brModelo NG`. No Flatpak, procure a pasta de estado dentro dos dados do aplicativo, geralmente sob `~/.var/app/io.github.kpagnussat.brModeloNG/`.

O arquivo é um conjunto de snapshots para recuperação, não um `.brM3` comum. A restauração é oferecida na próxima abertura após um encerramento incorreto. Confira **Intervalo** em **Configuração**: zero desativa o mecanismo. Salve os trabalhos restaurados em arquivos próprios. Não use a recuperação como substituta de backups.

## Como restaurar as preferências?

1. Feche o NG para evitar que ele regrave as preferências ao sair.
2. Faça uma cópia de **`config.chc`** na pasta de configurações indicada em [Primeiros passos](primeiros-passos.md#onde-ficam-as-preferências).
3. Renomeie esse arquivo e abra o programa. Ele voltará aos padrões.
4. Se existir um `config.chc` legado na pasta de execução, ele poderá ser importado novamente. Renomeie essa cópia também se quiser um início limpo.

Renomear apenas as preferências preserva os seus diagramas, a recuperação e o repositório de partes. Mantenha a cópia antiga para poder desfazer o reset.

## Tema errado ou texto difícil de ler

1. Abra **Editar → Aparência…**.
2. Escolha um tema fixo para experimentar o resultado.
3. Se optar por seguir o sistema, confira os preferidos claro e escuro. O sistema precisa informar seu modo ao aplicativo para a alternância automática funcionar.
4. Para alternar rapidamente, abra o botão de tema na barra principal e escolha **Claro** ou **Escuro**.

A ajuda no navegador segue `prefers-color-scheme` do navegador/sistema. Ela não lê a preferência de tema do editor.

## A ajuda não abriu

O menu **Ajuda → Ajuda** extrai o site incluído na instalação para a pasta de estado do NG. Se o lançamento do navegador falhar, uma janela mostra o endereço e o caminho local para copiar. Abra o `index.html` local manualmente se necessário.

No Flatpak, o navegador recebe a ajuda inteira numa única página, com as imagens incluídas, pelo portal do sistema. Se a instalação não contiver os recursos, a janela informa a falha de extração; reinstale um pacote completo.

## O arquivo demora a abrir ou foi recusado

Cada `.brM3` ou `.brMj` é lido por um processo separado, que tem limite de memória e de tempo; assim, um arquivo danificado ou malicioso não trava o NG. A janela continua respondendo durante a leitura. Se ela passar de meio segundo, aparece **Abrindo diagrama**, com **Cancelar**, que desiste da abertura na hora.

Se o arquivo for recusado, nada é aberto e o motivo fica no indicador de log. Os motivos mais comuns são arquivo danificado, tamanho acima do limite (64 MiB para `.brM3`, 128 MiB para `.brMj`) ou leitura que passou de 30 segundos. Um diagrama legítimo muito grande, com milhares de itens, leva alguns segundos, bem abaixo desse prazo.

## Apareceu um aviso de sessão X11

Na sessão X11 do Linux, qualquer programa com janela pode enviar teclas a outras janelas, inclusive a um terminal. O NG lê os arquivos recebidos num processo sem tela, mas avisa uma vez, porque um defeito ainda desconhecido na janela poderia ser usado para digitar comandos. Para abrir arquivos de outras pessoas, escolha a sessão Wayland na tela de login; o aviso não volta a aparecer.

## No Flatpak, um arquivo recente não abre

O Flatpak não tem acesso direto às suas pastas: cada arquivo chega pelo seletor do sistema ou pelo gerenciador de arquivos. Arquivos que entraram nos recentes antes da versão 1.0.1, quando o Flatpak lia a pasta Documentos, podem não abrir pela lista. Abra cada um uma vez por **Arquivo → Abrir**; depois disso, ele volta a abrir pelos recentes.

## Uma operação falhou

Clique no indicador de log da barra de estado e leia a mensagem relacionada à operação. Ao relatar um problema, informe versão (em **Ajuda → Sobre**), sistema, tipo de diagrama e passos para reproduzir. Use um exemplo inventado que demonstre a falha e preserve uma cópia do arquivo original.

Consulte [Imprimir e exportar](imprimir-exportar.md) para problemas de entrega e [Modelo lógico](modelo-logico.md) para revisar a conversão.
