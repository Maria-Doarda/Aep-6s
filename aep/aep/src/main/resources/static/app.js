/* AEP6S — registro de doações de alimentos.
   Aplicação de uma página só, com rotas por hash (#/, #/doadores, #/doadores/ID, #/doacoes).
   Fala com a própria API por caminhos relativos (/api/...), então não existe CORS. */

const API = '/api';
const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

const estado = {
  usuarios: [],
  doacoes: [],
  resumo: null,
  carregado: false,
};

const filtros = {
  buscaDoador: '',
  item: '',
  doador: '',
  de: '',
  ate: '',
  ordem: 'data-desc',
};

let rotaAtual = { nome: 'painel', param: null };

class ApiError extends Error {
  constructor(mensagem, status, corpo) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
    this.corpo = corpo;
  }
}

/* ---------------- utilidades ---------------- */

const id = (nome) => document.getElementById(nome);

function criar(tag, classe, texto) {
  const no = document.createElement(tag);
  if (classe) no.className = classe;
  if (texto !== undefined) no.textContent = texto;
  return no;
}

function numero(valor) {
  return new Intl.NumberFormat('pt-BR').format(valor ?? 0);
}

function formatarData(iso) {
  if (!iso) return '—';
  const partes = String(iso).split('-');
  if (partes.length !== 3) return iso;
  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

function hojeIso() {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

function semAcento(texto) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

function plural(n, singular, pluralTexto) {
  return `${numero(n)} ${n === 1 ? singular : pluralTexto}`;
}

/* ---------------- status, espera e avisos ---------------- */

function definirStatus(novoEstado) {
  const bloco = id('api-status');
  const texto = id('api-status-texto');
  bloco.dataset.state = novoEstado;
  if (novoEstado === 'online') texto.textContent = 'API conectada';
  else if (novoEstado === 'offline') texto.textContent = 'API indisponível';
  else texto.textContent = 'Conectando à API…';
}

let requisicoesPendentes = 0;
let timerAvisoLento = null;

function marcarEspera(delta) {
  requisicoesPendentes = Math.max(0, requisicoesPendentes + delta);
  if (requisicoesPendentes > 0) {
    if (!timerAvisoLento) {
      timerAvisoLento = window.setTimeout(() => {
        id('aviso-lento').hidden = false;
      }, 4000);
    }
    return;
  }
  window.clearTimeout(timerAvisoLento);
  timerAvisoLento = null;
  id('aviso-lento').hidden = true;
}

function toast(mensagem, tipo = 'ok') {
  const caixa = criar('div', tipo === 'erro' ? 'toast toast--erro' : 'toast', mensagem);
  id('toasts').append(caixa);
  window.setTimeout(() => {
    caixa.classList.add('toast--saindo');
    window.setTimeout(() => caixa.remove(), 220);
  }, 4200);
}

/* ---------------- cliente da API ---------------- */

async function api(caminho, opcoes = {}) {
  const config = { ...opcoes, headers: { ...(opcoes.headers || {}) } };
  if (config.body !== undefined) {
    config.headers['Content-Type'] = 'application/json';
    config.body = JSON.stringify(config.body);
  }

  marcarEspera(1);
  let resposta;
  try {
    resposta = await fetch(API + caminho, config);
  } catch (erro) {
    definirStatus('offline');
    throw new ApiError('Não foi possível falar com a API. Verifique a conexão e tente de novo.', 0, null);
  } finally {
    marcarEspera(-1);
  }

  definirStatus('online');

  if (resposta.status === 204) return null;

  const tipo = resposta.headers.get('content-type') || '';
  const corpo = tipo.includes('application/json')
    ? await resposta.json().catch(() => null)
    : null;

  if (!resposta.ok) {
    throw new ApiError(mensagemDoErro(resposta.status, corpo), resposta.status, corpo);
  }
  return corpo;
}

function mensagemDoErro(status, corpo) {
  if (corpo && typeof corpo === 'object') {
    if (typeof corpo.erro === 'string' && corpo.erro) return corpo.erro;
    const mensagens = Object.values(corpo).filter((v) => typeof v === 'string');
    if (mensagens.length === 1) return mensagens[0];
    if (mensagens.length > 1) return 'Confira os campos destacados.';
  }
  if (status === 404) return 'Registro não encontrado. Ele pode ter sido removido em outra aba.';
  return `A API respondeu com erro ${status}.`;
}

/* ---------------- dados derivados ---------------- */

function doadorPorId(usuarioId) {
  return estado.usuarios.find((u) => u.id === usuarioId) || null;
}

function nomeDoDoador(usuarioId) {
  const doador = doadorPorId(usuarioId);
  return doador ? doador.nome : null;
}

function doacoesDoDoador(usuarioId) {
  return estado.doacoes.filter((d) => d.usuarioId === usuarioId);
}

function somaQuantidade(lista) {
  return lista.reduce((total, d) => total + (Number(d.quantidade) || 0), 0);
}

function porDataDesc(a, b) {
  const dataA = a.dataDoacao || '';
  const dataB = b.dataDoacao || '';
  if (dataA !== dataB) return dataB.localeCompare(dataA);
  return String(a.item || '').localeCompare(String(b.item || ''), 'pt-BR');
}

/* ---------------- carregamento ---------------- */

async function carregarTudo() {
  const falha = id('falha');
  try {
    const [resumo, usuarios, doacoes] = await Promise.all([
      api('/doacoes/resumo'),
      api('/usuarios'),
      api('/doacoes'),
    ]);
    estado.resumo = resumo;
    estado.usuarios = Array.isArray(usuarios) ? usuarios : [];
    estado.doacoes = Array.isArray(doacoes) ? doacoes : [];
    estado.carregado = true;
    falha.hidden = true;
    renderTudo();
  } catch (erro) {
    definirStatus('offline');
    id('falha-texto').textContent = erro.message;
    falha.hidden = false;
  }
}

function renderTudo() {
  renderPainel();
  renderDoadores();
  renderPerfil();
  renderFiltroDoador();
  renderDoacoes();
  preencherDoadores();
  preencherSugestoes();
}

/* ---------------- rotas ---------------- */

function lerRota() {
  const partes = window.location.hash.replace(/^#\/?/, '').split('/').filter(Boolean);
  if (partes[0] === 'doadores' && partes[1]) {
    return { nome: 'perfil', param: decodeURIComponent(partes[1]) };
  }
  if (partes[0] === 'doadores') return { nome: 'doadores', param: null };
  if (partes[0] === 'doacoes') return { nome: 'doacoes', param: null };
  return { nome: 'painel', param: null };
}

function rotear(primeira = false) {
  const anterior = rotaAtual;
  rotaAtual = lerRota();

  for (const secao of document.querySelectorAll('[data-view]')) {
    secao.hidden = secao.dataset.view !== rotaAtual.nome;
  }

  const ativo = rotaAtual.nome === 'perfil' ? 'doadores' : rotaAtual.nome;
  for (const link of document.querySelectorAll('[data-nav]')) {
    if (link.dataset.nav === ativo) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  }

  if (rotaAtual.nome === 'perfil') renderPerfil();

  const titulos = {
    painel: 'Painel',
    doadores: 'Doadores',
    perfil: 'Doador',
    doacoes: 'Doações',
  };
  document.title = `${titulos[rotaAtual.nome]} — AEP6S`;

  const mudou = anterior.nome !== rotaAtual.nome || anterior.param !== rotaAtual.param;
  if (!primeira && mudou) {
    window.scrollTo(0, 0);
    const titulo = document.querySelector(`[data-view="${rotaAtual.nome}"] h1`);
    if (titulo) titulo.focus({ preventScroll: true });
  }
}

window.addEventListener('hashchange', () => rotear());

/* ---------------- painel ---------------- */

function renderPainel() {
  const resumo = estado.resumo || { totalDoacoes: 0, totalQuantidade: 0, itensDistintos: 0 };
  id('metric-doacoes').textContent = numero(resumo.totalDoacoes);
  id('metric-quantidade').textContent = numero(resumo.totalQuantidade);
  id('metric-tipos').textContent = numero(resumo.itensDistintos);
  id('metric-doadores').textContent = numero(estado.usuarios.length);

  renderBarrasItens();
  renderBarrasMeses();
  renderRecentes();
}

function desenharBarras(lista, linhas) {
  lista.replaceChildren();
  const maior = Math.max(1, ...linhas.map((l) => l.valor));
  for (const linha of linhas) {
    const item = criar('li', 'barra');
    const rotulo = criar('span', 'barra__rotulo', linha.rotulo);
    const trilho = criar('span', 'barra__trilho');
    const preenchimento = criar('span', 'barra__fill');
    preenchimento.style.width = `${(linha.valor / maior) * 100}%`;
    trilho.append(preenchimento);
    const valor = criar('span', 'barra__valor', numero(linha.valor));
    item.append(rotulo, trilho, valor);
    lista.append(item);
  }
}

function renderBarrasItens() {
  const totais = new Map();
  for (const doacao of estado.doacoes) {
    const chave = semAcento(doacao.item);
    if (!chave) continue;
    const atual = totais.get(chave) || { rotulo: doacao.item.trim(), valor: 0 };
    atual.valor += Number(doacao.quantidade) || 0;
    totais.set(chave, atual);
  }
  const ranking = [...totais.values()].sort((a, b) => b.valor - a.valor).slice(0, 5);

  id('itens-vazio').hidden = ranking.length > 0;
  id('barras-itens').hidden = ranking.length === 0;
  desenharBarras(id('barras-itens'), ranking);
}

function renderBarrasMeses() {
  const hoje = new Date();
  const meses = [];
  for (let i = 5; i >= 0; i -= 1) {
    const data = new Date(hoje.getFullYear(), hoje.getMonth() - i, 1);
    const chave = `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}`;
    const rotulo = `${MESES[data.getMonth()]}/${String(data.getFullYear()).slice(2)}`;
    meses.push({ chave, rotulo, valor: 0 });
  }
  for (const doacao of estado.doacoes) {
    const mes = meses.find((m) => m.chave === String(doacao.dataDoacao || '').slice(0, 7));
    if (mes) mes.valor += Number(doacao.quantidade) || 0;
  }
  desenharBarras(id('barras-meses'), meses);
}

function renderRecentes() {
  const corpo = id('recentes-corpo');
  corpo.replaceChildren();

  const recentes = [...estado.doacoes].sort(porDataDesc).slice(0, 5);
  id('recentes-vazio').hidden = recentes.length > 0;
  id('tabela-recentes').hidden = recentes.length === 0;

  for (const doacao of recentes) {
    const linha = criar('tr');
    linha.append(
      celula('cell-sub', formatarData(doacao.dataDoacao)),
      celula('cell-strong', doacao.item),
      celulaClasse('cell-qtd', numero(doacao.quantidade)),
      celulaDoador(doacao.usuarioId),
    );
    corpo.append(linha);
  }
}

/* ---------------- células de tabela ---------------- */

function celula(classeSpan, texto) {
  const td = criar('td');
  td.append(criar('span', classeSpan, texto));
  return td;
}

function celulaClasse(classe, texto) {
  return criar('td', classe, texto);
}

function celulaDoador(usuarioId) {
  const td = criar('td');
  const doador = doadorPorId(usuarioId);
  if (doador) {
    const link = criar('a', 'link', doador.nome);
    link.href = `#/doadores/${encodeURIComponent(doador.id)}`;
    td.append(link);
  } else {
    td.append(criar('span', 'cell-sub', 'Doador removido'));
  }
  return td;
}

function botaoLink(texto, rotulo, aoClicar, perigo = false) {
  const botao = criar('button', perigo ? 'link-danger' : 'link-acao', texto);
  botao.type = 'button';
  botao.setAttribute('aria-label', rotulo);
  botao.addEventListener('click', aoClicar);
  return botao;
}

/* ---------------- doadores ---------------- */

function renderDoadores() {
  const corpo = id('usuarios-corpo');
  corpo.replaceChildren();

  const termo = semAcento(filtros.buscaDoador);
  const lista = estado.usuarios
    .filter((u) => !termo || semAcento(u.nome).includes(termo) || semAcento(u.email).includes(termo))
    .sort((a, b) => String(a.nome).localeCompare(String(b.nome), 'pt-BR'));

  const semCadastro = estado.usuarios.length === 0;
  const semResultado = !semCadastro && lista.length === 0;

  id('tabela-usuarios').hidden = lista.length === 0;
  const vazio = id('usuarios-vazio');
  vazio.hidden = lista.length > 0;
  if (semCadastro) vazio.textContent = 'Nenhum doador cadastrado ainda. Use “Novo doador” para começar.';
  else if (semResultado) vazio.textContent = 'Nenhum doador encontrado para essa busca.';

  id('doadores-contagem').textContent = semCadastro
    ? ''
    : `${plural(lista.length, 'doador', 'doadores')}${termo ? ` de ${estado.usuarios.length}` : ''}`;

  for (const usuario of lista) {
    const linha = criar('tr');

    const celulaNome = criar('td');
    const link = criar('a', 'cell-strong link', usuario.nome);
    link.href = `#/doadores/${encodeURIComponent(usuario.id)}`;
    celulaNome.append(link, criar('span', 'cell-sub', usuario.email));

    const celulaEnderecos = criar('td');
    const enderecos = usuario.enderecos || [];
    if (enderecos.length === 0) {
      celulaEnderecos.append(criar('span', 'cell-sub', 'Sem endereço cadastrado'));
    } else {
      for (const endereco of enderecos) {
        celulaEnderecos.append(
          criar('span', 'cell-sub', `${endereco.rua} — ${endereco.cidade}/${endereco.estado}`),
        );
      }
    }

    const celulaAcao = criar('td', 'cell-acao');
    celulaAcao.append(
      botaoLink('Ver', `Ver o perfil de ${usuario.nome}`, () => {
        window.location.hash = `#/doadores/${encodeURIComponent(usuario.id)}`;
      }),
      botaoLink('Editar', `Editar o doador ${usuario.nome}`, () => abrirUsuario(usuario)),
      botaoLink('Excluir', `Excluir o doador ${usuario.nome}`, () => excluirUsuario(usuario), true),
    );

    linha.append(
      celulaNome,
      celulaEnderecos,
      celulaClasse('cell-qtd', numero(doacoesDoDoador(usuario.id).length)),
      celulaAcao,
    );
    corpo.append(linha);
  }
}

/* ---------------- perfil do doador ---------------- */

function renderPerfil() {
  const conteudo = id('perfil-conteudo');
  const naoAchado = id('perfil-naoachado');

  if (rotaAtual.nome !== 'perfil') return;

  const usuario = doadorPorId(rotaAtual.param);
  if (!usuario) {
    conteudo.hidden = true;
    naoAchado.hidden = !estado.carregado;
    return;
  }
  conteudo.hidden = false;
  naoAchado.hidden = true;

  id('t-perfil').textContent = usuario.nome;
  id('perfil-email').textContent = usuario.email;

  const doacoes = doacoesDoDoador(usuario.id).sort(porDataDesc);
  id('perfil-doacoes').textContent = numero(doacoes.length);
  id('perfil-quantidade').textContent = numero(somaQuantidade(doacoes));
  id('perfil-ultima').textContent = doacoes.length ? formatarData(doacoes[0].dataDoacao) : '—';

  const listaEnderecos = id('perfil-enderecos');
  listaEnderecos.replaceChildren();
  const enderecos = usuario.enderecos || [];
  if (enderecos.length === 0) {
    listaEnderecos.append(criar('li', 'vazio vazio--solo', 'Nenhum endereço cadastrado.'));
  }
  for (const endereco of enderecos) {
    const item = criar('li');
    item.append(
      criar('span', 'cell-strong', endereco.rua),
      criar('span', 'cell-sub', `${endereco.cidade}/${endereco.estado}`),
    );
    listaEnderecos.append(item);
  }

  const corpo = id('perfil-corpo');
  corpo.replaceChildren();
  id('tabela-perfil').hidden = doacoes.length === 0;
  id('perfil-vazio').hidden = doacoes.length > 0;

  for (const doacao of doacoes) {
    const linha = criar('tr');
    const acao = criar('td', 'cell-acao');
    acao.append(
      botaoLink('Editar', `Editar a doação de ${doacao.item}`, () => abrirDoacao({ doacao })),
      botaoLink('Excluir', `Excluir a doação de ${doacao.item}`, () => excluirDoacao(doacao), true),
    );
    linha.append(
      celula('cell-sub', formatarData(doacao.dataDoacao)),
      celula('cell-strong', doacao.item),
      celulaClasse('cell-qtd', numero(doacao.quantidade)),
      acao,
    );
    corpo.append(linha);
  }
}

id('perfil-doar').addEventListener('click', () => abrirDoacao({ usuarioId: rotaAtual.param }));
id('perfil-editar').addEventListener('click', () => {
  const usuario = doadorPorId(rotaAtual.param);
  if (usuario) abrirUsuario(usuario);
});
id('perfil-excluir').addEventListener('click', () => {
  const usuario = doadorPorId(rotaAtual.param);
  if (usuario) excluirUsuario(usuario, true);
});

/* ---------------- doações (lista e filtros) ---------------- */

function renderFiltroDoador() {
  const select = id('f-doador');
  select.replaceChildren();
  const todos = criar('option', null, 'Todos');
  todos.value = '';
  select.append(todos);

  const ordenados = [...estado.usuarios].sort((a, b) =>
    String(a.nome).localeCompare(String(b.nome), 'pt-BR'));
  for (const usuario of ordenados) {
    const opcao = criar('option', null, usuario.nome);
    opcao.value = usuario.id;
    select.append(opcao);
  }
  if (!estado.usuarios.some((u) => u.id === filtros.doador)) filtros.doador = '';
  select.value = filtros.doador;
}

function doacoesFiltradas() {
  const termo = semAcento(filtros.item);
  const lista = estado.doacoes.filter((d) => {
    if (termo && !semAcento(d.item).includes(termo)) return false;
    if (filtros.doador && d.usuarioId !== filtros.doador) return false;
    if (filtros.de && (d.dataDoacao || '') < filtros.de) return false;
    if (filtros.ate && (d.dataDoacao || '') > filtros.ate) return false;
    return true;
  });

  const ordenacoes = {
    'data-desc': porDataDesc,
    'data-asc': (a, b) => -porDataDesc(a, b),
    'qtd-desc': (a, b) => (Number(b.quantidade) || 0) - (Number(a.quantidade) || 0) || porDataDesc(a, b),
    'item-asc': (a, b) => String(a.item).localeCompare(String(b.item), 'pt-BR') || porDataDesc(a, b),
  };
  return lista.sort(ordenacoes[filtros.ordem] || porDataDesc);
}

function renderDoacoes() {
  const corpo = id('doacoes-corpo');
  corpo.replaceChildren();

  const lista = doacoesFiltradas();
  const semRegistro = estado.doacoes.length === 0;
  const filtrando = Boolean(filtros.item || filtros.doador || filtros.de || filtros.ate);

  id('tabela-doacoes').hidden = lista.length === 0;
  const vazio = id('doacoes-vazio');
  vazio.hidden = lista.length > 0;
  vazio.textContent = semRegistro
    ? 'Nenhuma doação registrada ainda. Use “Nova doação” para começar.'
    : 'Nenhuma doação encontrada com esses filtros.';

  id('doacoes-contagem').textContent = semRegistro
    ? ''
    : `${plural(lista.length, 'doação', 'doações')}${filtrando ? ` de ${estado.doacoes.length}` : ''}` +
      (lista.length ? ` · ${plural(somaQuantidade(lista), 'item', 'itens')}` : '');

  id('exportar-csv').disabled = lista.length === 0;

  for (const doacao of lista) {
    const linha = criar('tr');
    const acao = criar('td', 'cell-acao');
    acao.append(
      botaoLink('Editar', `Editar a doação de ${doacao.item}`, () => abrirDoacao({ doacao })),
      botaoLink('Excluir', `Excluir a doação de ${doacao.item}`, () => excluirDoacao(doacao), true),
    );
    linha.append(
      celula('cell-sub', formatarData(doacao.dataDoacao)),
      celula('cell-strong', doacao.item),
      celulaClasse('cell-qtd', numero(doacao.quantidade)),
      celulaDoador(doacao.usuarioId),
      acao,
    );
    corpo.append(linha);
  }
}

id('busca-doador').addEventListener('input', (evento) => {
  filtros.buscaDoador = evento.target.value;
  renderDoadores();
});

const ligacoesFiltro = {
  'f-item': 'item',
  'f-doador': 'doador',
  'f-de': 'de',
  'f-ate': 'ate',
  'f-ordem': 'ordem',
};
for (const [campo, chave] of Object.entries(ligacoesFiltro)) {
  id(campo).addEventListener('input', (evento) => {
    filtros[chave] = evento.target.value;
    renderDoacoes();
  });
}

id('f-limpar').addEventListener('click', () => {
  Object.assign(filtros, { item: '', doador: '', de: '', ate: '', ordem: 'data-desc' });
  id('f-item').value = '';
  id('f-doador').value = '';
  id('f-de').value = '';
  id('f-ate').value = '';
  id('f-ordem').value = 'data-desc';
  renderDoacoes();
});

function campoCsv(valor) {
  let texto = String(valor ?? '');
  // evita que o Excel interprete o texto como fórmula
  if (/^[=+\-@\t\r]/.test(texto)) texto = `'${texto}`;
  return `"${texto.replace(/"/g, '""')}"`;
}

id('exportar-csv').addEventListener('click', () => {
  const lista = doacoesFiltradas();
  const linhas = [['Data', 'Item', 'Quantidade', 'Doador', 'E-mail']];
  for (const d of lista) {
    const doador = doadorPorId(d.usuarioId);
    linhas.push([
      formatarData(d.dataDoacao),
      d.item,
      d.quantidade,
      doador ? doador.nome : 'Doador removido',
      doador ? doador.email : '',
    ]);
  }
  const texto = linhas.map((l) => l.map(campoCsv).join(';')).join('\r\n');
  const arquivo = new Blob(['﻿', texto], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(arquivo);
  const link = criar('a');
  link.href = url;
  link.download = `doacoes-${hojeIso()}.csv`;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  toast(`${plural(lista.length, 'doação exportada', 'doações exportadas')}.`);
});

/* ---------------- campos auxiliares da doação ---------------- */

function preencherDoadores() {
  const select = id('d-usuario');
  const selecionado = select.value;
  select.replaceChildren();

  const inicial = criar('option', null, 'Selecione um doador');
  inicial.value = '';
  select.append(inicial);

  const ordenados = [...estado.usuarios].sort((a, b) =>
    String(a.nome).localeCompare(String(b.nome), 'pt-BR'));
  for (const usuario of ordenados) {
    const opcao = criar('option', null, `${usuario.nome} — ${usuario.email}`);
    opcao.value = usuario.id;
    select.append(opcao);
  }

  const aindaExiste = estado.usuarios.some((u) => u.id === selecionado);
  select.value = aindaExiste ? selecionado : '';
}

function preencherSugestoes() {
  const lista = id('itens-sugeridos');
  lista.replaceChildren();
  const vistos = new Map();
  for (const doacao of estado.doacoes) {
    const chave = semAcento(doacao.item);
    if (chave && !vistos.has(chave)) vistos.set(chave, doacao.item.trim());
  }
  for (const nome of [...vistos.values()].sort((a, b) => a.localeCompare(b, 'pt-BR'))) {
    const opcao = criar('option');
    opcao.value = nome;
    lista.append(opcao);
  }
}

/* ---------------- erros de formulário ---------------- */

function limparErros(formulario) {
  for (const campo of formulario.querySelectorAll('[aria-invalid="true"]')) {
    campo.removeAttribute('aria-invalid');
  }
  for (const erro of formulario.querySelectorAll('.field__error')) {
    erro.textContent = '';
    erro.hidden = true;
  }
}

function mostrarErroCampo(formulario, chave, mensagem) {
  const erro = formulario.querySelector(`[data-erro="${chave}"]`);
  const campo = formulario.querySelector(`[data-campo="${chave}"]`);

  if (erro) {
    erro.textContent = mensagem;
    erro.hidden = false;
  }
  if (campo) {
    campo.setAttribute('aria-invalid', 'true');
    if (!campo.getAttribute('aria-describedby') && erro && erro.id) {
      campo.setAttribute('aria-describedby', erro.id);
    }
  }
  return Boolean(erro || campo);
}

function normalizarChave(chave) {
  return String(chave).replace(/\[(\d+)\]\./g, '-$1-');
}

// Limpa o erro do campo assim que a pessoa começa a corrigir
for (const formulario of [id('form-usuario'), id('form-doacao')]) {
  formulario.addEventListener('input', (evento) => {
    const campo = evento.target;
    if (campo.getAttribute('aria-invalid') !== 'true') return;
    campo.removeAttribute('aria-invalid');
    const chave = campo.dataset.campo;
    const erro = chave ? formulario.querySelector(`[data-erro="${chave}"]`) : null;
    if (erro) {
      erro.textContent = '';
      erro.hidden = true;
    }
  });
}

function tratarErroDeFormulario(erro, formulario) {
  if (erro instanceof ApiError && erro.corpo && typeof erro.corpo === 'object' && !erro.corpo.erro) {
    let marcouAlgum = false;
    for (const [chave, mensagem] of Object.entries(erro.corpo)) {
      if (typeof mensagem !== 'string') continue;
      if (mostrarErroCampo(formulario, normalizarChave(chave), mensagem)) marcouAlgum = true;
    }
    if (marcouAlgum) {
      toast('Confira os campos destacados.', 'erro');
      return;
    }
  }
  toast(erro.message, 'erro');
}

/* ---------------- endereços dinâmicos ---------------- */

function reindexarEnderecos() {
  const blocos = [...id('enderecos-lista').children];
  blocos.forEach((bloco, indice) => {
    bloco.querySelector('.endereco__numero').textContent = `Endereço ${indice + 1}`;
    for (const nomeCampo of ['rua', 'cidade', 'estado']) {
      const chave = `enderecos-${indice}-${nomeCampo}`;
      const campo = bloco.querySelector(`[data-campo-base="${nomeCampo}"]`);
      const erro = bloco.querySelector(`[data-erro-base="${nomeCampo}"]`);
      campo.dataset.campo = chave;
      campo.id = `endereco-${indice}-${nomeCampo}`;
      erro.dataset.erro = chave;
      erro.id = `erro-${chave}`;
      const rotulo = campo.closest('.field').querySelector('label');
      if (rotulo) rotulo.setAttribute('for', campo.id);
    }
  });
}

function adicionarEndereco(dados = {}) {
  const bloco = document.createElement('div');
  bloco.className = 'endereco';
  bloco.innerHTML = `
    <div class="endereco__topo">
      <span class="endereco__numero"></span>
      <button class="link-danger" type="button" data-remover>Remover</button>
    </div>
    <div class="field">
      <label>Rua e número</label>
      <input type="text" maxlength="100" data-campo-base="rua" autocomplete="off">
      <p class="field__error" data-erro-base="rua" hidden></p>
    </div>
    <div class="field-row">
      <div class="field">
        <label>Cidade</label>
        <input type="text" maxlength="50" data-campo-base="cidade" autocomplete="off">
        <p class="field__error" data-erro-base="cidade" hidden></p>
      </div>
      <div class="field">
        <label>UF</label>
        <input type="text" maxlength="2" data-campo-base="estado" autocomplete="off">
        <p class="field__error" data-erro-base="estado" hidden></p>
      </div>
    </div>`;

  bloco.querySelector('[data-campo-base="rua"]').value = dados.rua || '';
  bloco.querySelector('[data-campo-base="cidade"]').value = dados.cidade || '';

  const uf = bloco.querySelector('[data-campo-base="estado"]');
  uf.value = dados.estado || '';
  uf.addEventListener('input', () => {
    uf.value = uf.value.toUpperCase();
  });

  bloco.querySelector('[data-remover]').addEventListener('click', () => {
    bloco.remove();
    reindexarEnderecos();
  });

  id('enderecos-lista').append(bloco);
  reindexarEnderecos();
}

function lerEnderecos(formulario) {
  const blocos = [...id('enderecos-lista').children];
  const enderecos = [];

  for (const [indice, bloco] of blocos.entries()) {
    const valor = (nomeCampo) =>
      bloco.querySelector(`[data-campo-base="${nomeCampo}"]`).value.trim();

    const rua = valor('rua');
    const cidade = valor('cidade');
    const uf = valor('estado').toUpperCase();

    if (!rua && !cidade && !uf) continue;

    if (!rua || !cidade || !uf) {
      const faltando = !rua ? 'rua' : !cidade ? 'cidade' : 'estado';
      mostrarErroCampo(
        formulario,
        `enderecos-${indice}-${faltando}`,
        'Preencha rua, cidade e UF — ou remova este endereço.',
      );
      return null;
    }

    if (uf.length !== 2) {
      mostrarErroCampo(formulario, `enderecos-${indice}-estado`, 'Use a sigla do estado com 2 letras (ex.: PR).');
      return null;
    }

    enderecos.push({ rua, cidade, estado: uf });
  }

  return enderecos;
}

/* ---------------- diálogos ---------------- */

function alternarBotao(botao, ocupado, textoOcupado) {
  if (ocupado) {
    botao.dataset.textoOriginal = botao.textContent;
    botao.textContent = textoOcupado;
    botao.disabled = true;
  } else {
    botao.textContent = botao.dataset.textoOriginal || botao.textContent;
    botao.disabled = false;
  }
}

for (const dialogo of document.querySelectorAll('dialog.modal')) {
  // fechar pelo botão Cancelar ou clicando fora da ficha
  dialogo.addEventListener('click', (evento) => {
    if (evento.target === dialogo || evento.target.closest('[data-fechar]')) dialogo.close();
  });
}

let usuarioEmEdicao = null;
let doacaoEmEdicao = null;

function abrirUsuario(usuario = null) {
  usuarioEmEdicao = usuario;
  const formulario = id('form-usuario');
  limparErros(formulario);
  formulario.reset();
  id('enderecos-lista').replaceChildren();

  id('dlg-usuario-titulo').textContent = usuario ? 'Editar doador' : 'Novo doador';
  id('btn-usuario').textContent = usuario ? 'Salvar alterações' : 'Cadastrar doador';

  if (usuario) {
    id('u-nome').value = usuario.nome || '';
    id('u-email').value = usuario.email || '';
    for (const endereco of usuario.enderecos || []) adicionarEndereco(endereco);
  }
  if (!id('enderecos-lista').children.length) adicionarEndereco();

  id('dlg-usuario').showModal();
  id('u-nome').focus();
}

function abrirDoacao({ doacao = null, usuarioId = '' } = {}) {
  doacaoEmEdicao = doacao;
  const formulario = id('form-doacao');
  limparErros(formulario);
  formulario.reset();

  id('dlg-doacao-titulo').textContent = doacao ? 'Editar doação' : 'Nova doação';
  id('btn-doacao').textContent = doacao ? 'Salvar alterações' : 'Registrar doação';

  if (doacao) {
    id('d-usuario').value = doacao.usuarioId || '';
    id('d-item').value = doacao.item || '';
    id('d-quantidade').value = doacao.quantidade ?? 1;
    id('d-data').value = doacao.dataDoacao || hojeIso();
  } else {
    id('d-usuario').value = estado.usuarios.some((u) => u.id === usuarioId) ? usuarioId : '';
    id('d-quantidade').value = '1';
    id('d-data').value = hojeIso();
  }

  id('dlg-doacao').showModal();
  (id('d-usuario').value ? id('d-item') : id('d-usuario')).focus();
}

for (const botao of document.querySelectorAll('[data-abrir]')) {
  botao.addEventListener('click', () => {
    if (botao.dataset.abrir === 'usuario') abrirUsuario();
    else abrirDoacao();
  });
}

id('add-endereco').addEventListener('click', () => adicionarEndereco());

id('form-usuario').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const formulario = evento.currentTarget;
  limparErros(formulario);

  const enderecos = lerEnderecos(formulario);
  if (enderecos === null) {
    toast('Confira os endereços destacados.', 'erro');
    return;
  }

  const dados = {
    nome: id('u-nome').value.trim(),
    email: id('u-email').value.trim(),
    enderecos,
  };

  const editando = usuarioEmEdicao;
  const botao = id('btn-usuario');
  alternarBotao(botao, true, editando ? 'Salvando…' : 'Cadastrando…');
  try {
    if (editando) {
      await api(`/usuarios/${encodeURIComponent(editando.id)}`, { method: 'PUT', body: dados });
    } else {
      await api('/usuarios', { method: 'POST', body: dados });
    }
    id('dlg-usuario').close();
    toast(editando ? 'Doador atualizado.' : 'Doador cadastrado.');
    await carregarTudo();
  } catch (erro) {
    tratarErroDeFormulario(erro, formulario);
  } finally {
    alternarBotao(botao, false);
  }
});

id('form-doacao').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const formulario = evento.currentTarget;
  limparErros(formulario);

  const dados = {
    usuarioId: id('d-usuario').value,
    item: id('d-item').value.trim(),
    quantidade: Number(id('d-quantidade').value),
    dataDoacao: id('d-data').value || null,
  };

  const editando = doacaoEmEdicao;
  const botao = id('btn-doacao');
  alternarBotao(botao, true, editando ? 'Salvando…' : 'Registrando…');
  try {
    if (editando) {
      await api(`/doacoes/${encodeURIComponent(editando.id)}`, { method: 'PUT', body: dados });
    } else {
      await api('/doacoes', { method: 'POST', body: dados });
    }
    id('dlg-doacao').close();
    toast(editando ? 'Doação atualizada.' : 'Doação registrada.');
    await carregarTudo();
  } catch (erro) {
    tratarErroDeFormulario(erro, formulario);
  } finally {
    alternarBotao(botao, false);
  }
});

/* ---------------- exclusões ---------------- */

async function excluirUsuario(usuario, voltarParaLista = false) {
  const quantas = doacoesDoDoador(usuario.id).length;
  let aviso = '';
  if (quantas === 1) {
    aviso = '\n\n1 doação registrada para este doador continua no banco.';
  } else if (quantas > 1) {
    aviso = `\n\n${quantas} doações registradas para este doador continuam no banco.`;
  }

  if (!window.confirm(`Excluir o doador "${usuario.nome}"?${aviso}`)) return;

  try {
    await api(`/usuarios/${encodeURIComponent(usuario.id)}`, { method: 'DELETE' });
    toast('Doador excluído.');
    if (voltarParaLista) window.location.hash = '#/doadores';
    await carregarTudo();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

async function excluirDoacao(doacao) {
  const nome = nomeDoDoador(doacao.usuarioId);
  const detalhe = nome ? ` de ${nome}` : '';
  if (!window.confirm(`Excluir a doação de ${doacao.item}${detalhe}, de ${formatarData(doacao.dataDoacao)}?`)) return;

  try {
    await api(`/doacoes/${encodeURIComponent(doacao.id)}`, { method: 'DELETE' });
    toast('Doação excluída.');
    await carregarTudo();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

/* ---------------- inicialização ---------------- */

id('tentar-novamente').addEventListener('click', () => {
  definirStatus('connecting');
  carregarTudo();
});

// Clicar na quantidade seleciona o valor para não virar "17" ao digitar "7"
const campoQuantidade = id('d-quantidade');
campoQuantidade.addEventListener('focus', () => campoQuantidade.select());

definirStatus('connecting');
rotear(true);
carregarTudo();
