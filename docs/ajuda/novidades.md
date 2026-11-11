# Novidades do brModelo NG

O NG dá continuidade ao brModelo oficial **3.3.2**, com melhorias de interface e manutenção. Os modelos binários `.brM3` mantêm o formato compatível; o formato JSON `.brMj` é uma alternativa própria do NG.

## Interface

- Temas FlatLaf claros e escuros, ícones SVG e um seletor de **18 temas** em **Editar → Aparência…**. É possível seguir o sistema e escolher um preferido para cada modo.
- Troca de tema sem reiniciar; dicas mais descritivas nos botões e campos.
- Inspector com melhor edição das propriedades e apresentação de valores longos.
- Paleta que ajusta os botões à altura disponível e usa rolagem quando necessário; abas de diagrama com rolagem.
- Barra de estado com indicação de erros ainda não lidos e acesso ao log.
- Prévia de impressão e editores ajustados para os temas e a escala da interface.

## Arquivos e distribuição

O NG usa pastas próprias para preferências, recuperação e partes prontas. O auto-salvamento registra snapshots e escreve a recuperação sem fazer a gravação de disco no fluxo de interação da janela.

Arquivos binários e JSON passam por validação das classes permitidas e são lidos por um processo separado, com limite de memória e de tempo, para que um arquivo danificado ou malicioso não trave o programa. Durante a leitura, a janela continua respondendo, e uma abertura demorada pode ser cancelada. Ao salvar, o arquivo anterior só é substituído depois que a gravação termina. Isso não altera a regra de compartilhamento: use `.brM3` para o oficial; veja [Problemas comuns](problemas-comuns.md).

A ajuda agora é um site local gerado de Markdown e incluído no jar e nos pacotes. Ela abre no navegador em **Ajuda → Ajuda**, ou **F1**. O conteúdo também é legível no repositório, sem precisar do aplicativo.

## Créditos e lançamentos

**Ajuda → Sobre** apresenta os créditos e a versão instalada. **Ajuda → Releases do brModelo NG** abre a [página de lançamentos do NG](https://github.com/kpagnussat/brModeloNG/releases). Esse comando precisa de acesso à internet; a ajuda local não precisa.

Comece por [Primeiros passos](primeiros-passos.md) para experimentar o fluxo de edição.
