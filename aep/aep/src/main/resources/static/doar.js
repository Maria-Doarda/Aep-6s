/* AEP6S — página pública de doação.
   Fluxo em 4 etapas. Fala com a API da mesma origem (/api/...).
   Privacidade: esta página NUNCA lista doadores; só lê /api/doacoes (itens) e /api/doacoes/resumo. */

const params = new URLSearchParams(window.location.search);
const API = (params.get('api') || '') + '/api';
const FORCAR_DEMO = params.has('demo');
const CHAVE_DOADOR = 'aep6s-doador';
const TOTAL_PASSOS = 4;

/* Cópia (somente leitura) dos dados reais publicados no Render, usada no modo demonstração. */
const DEMO_DADOS = {
  resumo: { totalDoacoes: 6, totalQuantidade: 101, itensDistintos: 5 },
  doacoes: [
    { item: 'Arroz', quantidade: 10 },
    { item: 'Feijão', quantidade: 5 },
    { item: 'Leite', quantidade: 8 },
    { item: 'Cheetos', quantidade: 67 },
    { item: 'Macarrão', quantidade: 1 },
    { item: 'Leite', quantidade: 10 },
  ],
};

const ITENS_PADRAO = ['Arroz', 'Feijão', 'Leite', 'Macarrão', 'Óleo', 'Pão', 'Frutas'];
const MAX_CARTOES = 7;

const estado = {
  passo: 1,
  item: '',
  quantidade: 1,
  data: '',
  nome: '',
  email: '',
  usuarioId: null, // reaproveitado se o envio da doação falhar depois de criar o doador
  enviando: false,
  demo: FORCAR_DEMO,
};

const $ = (seletor, raiz = document) => raiz.querySelector(seletor);
const id = (nome) => document.getElementById(nome);

/* ---------------- utilidades ---------------- */

function criar(tag, classe, texto) {
  const no = document.createElement(tag);
  if (classe) no.className = classe;
  if (texto !== undefined) no.textContent = texto;
  return no;
}

function semAcento(texto) {
  return String(texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

function capitalizar(texto) {
  const t = String(texto ?? '').trim();
  return t ? t.charAt(0).toLocaleUpperCase('pt-BR') + t.slice(1) : t;
}

function iso(data) {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

function formatarData(valor) {
  const [a, m, d] = String(valor).split('-');
  return a && m && d ? `${d}/${m}/${a}` : valor;
}

function numero(n) {
  return new Intl.NumberFormat('pt-BR').format(n ?? 0);
}

function primeiroNome(nome) {
  return String(nome || '').trim().split(/\s+/)[0] || '';
}

function guardar(chave, valor) {
  try {
    if (valor === null) window.localStorage.removeItem(chave);
    else window.localStorage.setItem(chave, JSON.stringify(valor));
  } catch (erro) { /* armazenamento indisponível: segue sem lembrar */ }
}

function lerGuardado(chave) {
  try {
    return JSON.parse(window.localStorage.getItem(chave) || 'null');
  } catch (erro) {
    return null;
  }
}

/* ---------------- cliente da API (com modo demonstração) ---------------- */

class ApiError extends Error {
  constructor(mensagem, status, corpo) {
    super(mensagem);
    this.status = status;
    this.corpo = corpo;
  }
}

function ativarDemo() {
  estado.demo = true;
  id('faixa-demo').hidden = false;
}

function respostaDemo(caminho, opcoes) {
  const metodo = (opcoes.method || 'GET').toUpperCase();
  if (metodo === 'GET' && caminho.startsWith('/doacoes/resumo')) return DEMO_DADOS.resumo;
  if (metodo === 'GET' && caminho.startsWith('/doacoes')) return DEMO_DADOS.doacoes;
  const corpo = opcoes.body || {};
  const codigo = `demo${Date.now().toString(36)}`;
  return { id: codigo, ...corpo };
}

async function api(caminho, opcoes = {}) {
  if (estado.demo) {
    await new Promise((r) => window.setTimeout(r, 350));
    return respostaDemo(caminho, opcoes);
  }

  const config = { ...opcoes, headers: { ...(opcoes.headers || {}) } };
  const corpoOriginal = opcoes.body;
  if (config.body !== undefined) {
    config.headers['Content-Type'] = 'application/json';
    config.body = JSON.stringify(config.body);
  }

  const controle = new AbortController();
  const limite = window.setTimeout(() => controle.abort(), 90000);
  config.signal = controle.signal;

  let resposta;
  try {
    resposta = await fetch(API + caminho, config);
  } catch (erro) {
    ativarDemo();
    return respostaDemo(caminho, { ...opcoes, body: corpoOriginal });
  } finally {
    window.clearTimeout(limite);
  }

  const tipo = resposta.headers.get('content-type') || '';
  const ehJson = tipo.includes('application/json');

  // Hospedagem gratuita dormindo costuma devolver HTML ou 502/503/504
  if ([502, 503, 504].includes(resposta.status) || (resposta.ok && resposta.status !== 204 && !ehJson)) {
    ativarDemo();
    return respostaDemo(caminho, { ...opcoes, body: corpoOriginal });
  }

  if (resposta.status === 204) return null;
  const corpo = ehJson ? await resposta.json().catch(() => null) : null;
  if (!resposta.ok) throw new ApiError('Erro da API', resposta.status, corpo);
  return corpo;
}

/* ---------------- erros ---------------- */

const CAMPO_DO_ERRO = {
  item: { campo: 'item', erro: 'erro-item', passo: 1 },
  quantidade: { campo: 'qtd', erro: 'erro-qtd', passo: 2 },
  dataDoacao: { campo: 'data', erro: 'erro-data', passo: 2 },
  nome: { campo: 'nome', erro: 'erro-nome', passo: 3 },
  email: { campo: 'email', erro: 'erro-email', passo: 3 },
};

function mostrarErro(idErro, mensagem, idCampo) {
  const erro = id(idErro);
  erro.textContent = mensagem;
  erro.hidden = false;
  const campo = idCampo ? id(idCampo) : null;
  if (campo) campo.setAttribute('aria-invalid', 'true');
}

function limparErros() {
  for (const erro of document.querySelectorAll('.erro')) {
    erro.textContent = '';
    erro.hidden = true;
  }
  for (const campo of document.querySelectorAll('[aria-invalid]')) campo.removeAttribute('aria-invalid');
  id('itens').removeAttribute('aria-invalid');
}

/* ---------------- etapas ---------------- */

const NOMES_PASSO = ['Item', 'Quantidade', 'Seus dados', 'Revisão'];

function irParaPasso(novo, { foco = true } = {}) {
  estado.passo = novo;
  limparErros();

  for (const secao of document.querySelectorAll('fieldset.passo')) {
    secao.hidden = Number(secao.dataset.passo) !== novo;
  }

  for (const li of id('trilha').children) {
    const n = Number(li.dataset.passo);
    li.classList.toggle('feito', n < novo);
    li.classList.toggle('atual', n === novo);
    if (n === novo) li.setAttribute('aria-current', 'step');
    else li.removeAttribute('aria-current');
  }

  id('voltar').hidden = novo === 1;
  id('avancar').textContent = novo === TOTAL_PASSOS ? 'Confirmar doação' : 'Continuar';

  if (novo === TOTAL_PASSOS) preencherResumo();

  id('anuncio').textContent = `Etapa ${novo} de ${TOTAL_PASSOS}: ${NOMES_PASSO[novo - 1]}`;
  if (foco) {
    const legenda = $(`fieldset.passo[data-passo="${novo}"] legend`);
    if (legenda) legenda.focus({ preventScroll: true });
    id('cartao').scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }
}

function itemEscolhido() {
  const marcado = $('input[name="item"]:checked');
  if (!marcado) return '';
  return marcado.value === '__outro' ? id('outro').value.trim() : marcado.value;
}

function validarPasso(passo) {
  limparErros();

  if (passo === 1) {
    const item = itemEscolhido();
    if (!item) {
      const marcado = $('input[name="item"]:checked');
      if (marcado && marcado.value === '__outro') {
        mostrarErro('erro-item', 'Escreva qual item você quer doar.', 'outro');
        id('outro').focus();
      } else {
        mostrarErro('erro-item', 'Escolha o que você quer doar.');
        const primeiro = $('input[name="item"]');
        if (primeiro) primeiro.focus();
      }
      return false;
    }
    estado.item = capitalizar(item);
    return true;
  }

  if (passo === 2) {
    const qtd = Number(id('qtd').value);
    if (!Number.isInteger(qtd) || qtd < 1) {
      mostrarErro('erro-qtd', 'Informe uma quantidade maior que zero.', 'qtd');
      id('qtd').focus();
      return false;
    }
    if (qtd > 999) {
      mostrarErro('erro-qtd', 'Para quantidades acima de 999, divida em mais de uma doação.', 'qtd');
      id('qtd').focus();
      return false;
    }
    const data = id('data').value;
    const limite = new Date();
    limite.setDate(limite.getDate() + 30);
    if (!data || data > iso(limite)) {
      mostrarErro('erro-data', 'Escolha uma data válida (hoje ou nos próximos 30 dias).', 'data');
      id('data').focus();
      return false;
    }
    estado.quantidade = qtd;
    estado.data = data;
    return true;
  }

  if (passo === 3) {
    const nome = id('nome').value.trim();
    const email = id('email').value.trim();
    let primeiroInvalido = null;
    if (!nome) {
      mostrarErro('erro-nome', 'Diga como podemos chamar você.', 'nome');
      primeiroInvalido = id('nome');
    }
    if (!email || !id('email').validity.valid || !/.+@.+\..+/.test(email)) {
      mostrarErro('erro-email', 'Confira o e-mail: parece faltar algo (ex.: nome@email.com).', 'email');
      primeiroInvalido = primeiroInvalido || id('email');
    }
    if (primeiroInvalido) {
      primeiroInvalido.focus();
      return false;
    }
    estado.nome = nome;
    estado.email = email;
    return true;
  }

  return true;
}

function preencherResumo() {
  id('r-item').textContent = estado.item;
  id('r-qtd').textContent = numero(estado.quantidade);
  id('r-data').textContent = formatarData(estado.data);
  id('r-nome').textContent = estado.nome;
}

/* ---------------- envio ---------------- */

function doadorLembrado() {
  const salvo = lerGuardado(CHAVE_DOADOR);
  return salvo && salvo.id && salvo.email ? salvo : null;
}

async function resolverDoador() {
  if (estado.usuarioId) return estado.usuarioId;

  // Sem busca por e-mail na API (e sem listar doadores aqui): só reaproveita
  // o doador que este mesmo navegador já criou, se nome e e-mail não mudaram.
  const salvo = doadorLembrado();
  if (salvo && salvo.email.toLowerCase() === estado.email.toLowerCase() && salvo.nome === estado.nome) {
    estado.usuarioId = salvo.id;
    return salvo.id;
  }

  const criado = await api('/usuarios', {
    method: 'POST',
    body: { nome: estado.nome, email: estado.email, enderecos: [] },
  });
  estado.usuarioId = criado.id;
  guardar(CHAVE_DOADOR, { id: criado.id, nome: estado.nome, email: estado.email });
  return criado.id;
}

function tratarErroDeEnvio(erro) {
  const corpo = erro && erro.corpo;
  if (erro instanceof ApiError && erro.status === 400 && corpo && typeof corpo === 'object') {
    let primeiroPasso = null;
    for (const [chave, mensagem] of Object.entries(corpo)) {
      const alvo = CAMPO_DO_ERRO[chave];
      if (alvo && typeof mensagem === 'string') {
        primeiroPasso = primeiroPasso === null ? alvo.passo : Math.min(primeiroPasso, alvo.passo);
      }
    }
    if (primeiroPasso !== null) {
      irParaPasso(primeiroPasso);
      for (const [chave, mensagem] of Object.entries(corpo)) {
        const alvo = CAMPO_DO_ERRO[chave];
        if (alvo && typeof mensagem === 'string' && alvo.passo === primeiroPasso) {
          mostrarErro(alvo.erro, mensagem, alvo.campo);
        }
      }
      return;
    }
  }
  mostrarErro('erro-geral', 'Não conseguimos registrar agora. Seus dados continuam aqui, tente de novo.');
}

let timerLento = null;

async function enviar() {
  if (estado.enviando) return;
  estado.enviando = true;
  const botao = id('avancar');
  botao.disabled = true;
  botao.textContent = 'Enviando…';
  botao.setAttribute('aria-busy', 'true');
  limparErros();

  timerLento = window.setTimeout(() => { id('aviso-lento').hidden = false; }, 5000);

  try {
    const usuarioId = await resolverDoador();
    const doacao = await api('/doacoes', {
      method: 'POST',
      body: {
        usuarioId,
        item: estado.item,
        quantidade: estado.quantidade,
        dataDoacao: estado.data,
      },
    });
    mostrarSucesso(doacao);
  } catch (erro) {
    tratarErroDeEnvio(erro);
  } finally {
    window.clearTimeout(timerLento);
    id('aviso-lento').hidden = true;
    estado.enviando = false;
    botao.disabled = false;
    botao.removeAttribute('aria-busy');
    if (estado.passo === TOTAL_PASSOS) botao.textContent = 'Confirmar doação';
  }
}

function mostrarSucesso(doacao) {
  const nome = primeiroNome(estado.nome);
  id('sucesso-titulo').textContent = nome ? `Gratidão, ${nome}!` : 'Doação registrada!';
  id('sucesso-texto').textContent =
    `Sua doação de ${numero(estado.quantidade)} × ${estado.item} foi registrada.`;
  id('s-item').textContent = estado.item;
  id('s-qtd').textContent = numero(estado.quantidade);
  id('s-data').textContent = formatarData(estado.data);
  id('s-codigo').textContent = String(doacao.id || '').slice(-6).toUpperCase() || '—';

  id('cartao').hidden = true;
  id('sucesso').hidden = false;
  id('sucesso-titulo').focus({ preventScroll: true });
  window.scrollTo({ top: id('sucesso').offsetTop - 80, behavior: 'smooth' });
  id('anuncio').textContent = 'Doação registrada com sucesso.';

  atualizarImpacto(true);
}

/* ---------------- dados reais: itens mais doados e resumo ---------------- */

async function carregarItens() {
  let doacoes = [];
  try {
    const lista = await api('/doacoes');
    if (Array.isArray(lista)) doacoes = lista;
  } catch (erro) { /* mantém só os itens padrão */ }

  // frequência de cada item doado (somente o nome do item, nada de pessoas)
  const contagem = new Map();
  for (const d of doacoes) {
    const chave = semAcento(d.item);
    if (!chave) continue;
    const atual = contagem.get(chave) || { nome: capitalizar(d.item), vezes: 0 };
    atual.vezes += 1;
    contagem.set(chave, atual);
  }
  for (const nome of ITENS_PADRAO) {
    const chave = semAcento(nome);
    if (!contagem.has(chave)) contagem.set(chave, { nome, vezes: 0 });
    else contagem.get(chave).nome = nome; // mantém a grafia correta dos itens padrão
  }

  const ordenados = [...contagem.values()]
    .sort((a, b) => b.vezes - a.vezes || a.nome.localeCompare(b.nome, 'pt-BR'))
    .slice(0, MAX_CARTOES);

  desenharCartoes(ordenados);
}

function desenharCartoes(itens) {
  const caixa = id('itens');
  const selecionado = itemEscolhido();
  caixa.replaceChildren();

  for (const item of itens) {
    const rotulo = criar('label', 'item');
    const radio = criar('input');
    radio.type = 'radio';
    radio.name = 'item';
    radio.value = item.nome;
    const corpo = criar('span');
    const disco = criar('i', 'item__disco', item.nome.charAt(0).toLocaleUpperCase('pt-BR'));
    disco.setAttribute('aria-hidden', 'true');
    corpo.append(disco, document.createTextNode(item.nome));
    if (item.vezes > 1) corpo.append(criar('small', 'item__popular', 'Muito doado'));
    rotulo.append(radio, corpo);
    caixa.append(rotulo);
    if (selecionado && selecionado === item.nome) radio.checked = true;
  }

  const outro = criar('label', 'item');
  const radioOutro = criar('input');
  radioOutro.type = 'radio';
  radioOutro.name = 'item';
  radioOutro.value = '__outro';
  const corpoOutro = criar('span');
  const discoOutro = criar('i', 'item__disco', '+');
  discoOutro.setAttribute('aria-hidden', 'true');
  corpoOutro.append(discoOutro, document.createTextNode('Outro'));
  outro.append(radioOutro, corpoOutro);
  caixa.append(outro);
}

async function atualizarImpacto(aposDoar = false) {
  let resumo = null;
  try {
    resumo = await api('/doacoes/resumo');
  } catch (erro) { /* dado secundário: se falhar, some */ }

  const total = resumo && Number(resumo.totalDoacoes);
  const alvo = aposDoar ? id('sucesso-impacto') : id('impacto');
  if (!total) {
    alvo.hidden = true;
    return;
  }
  alvo.textContent = aposDoar
    ? `Você ajudou a chegar a ${numero(total)} ${total === 1 ? 'doação registrada' : 'doações registradas'}.`
    : `Já são ${numero(total)} ${total === 1 ? 'doação registrada' : 'doações registradas'}. A próxima pode ser a sua.`;
  alvo.hidden = false;
}

/* ---------------- eventos ---------------- */

id('form').addEventListener('submit', (evento) => {
  evento.preventDefault();
  if (!validarPasso(estado.passo)) return;
  if (estado.passo < TOTAL_PASSOS) irParaPasso(estado.passo + 1);
  else enviar();
});

id('voltar').addEventListener('click', () => {
  if (estado.passo > 1) irParaPasso(estado.passo - 1);
});

for (const botao of document.querySelectorAll('[data-editar]')) {
  botao.addEventListener('click', () => irParaPasso(Number(botao.dataset.editar)));
}

id('itens').addEventListener('change', () => {
  const marcado = $('input[name="item"]:checked');
  const outro = marcado && marcado.value === '__outro';
  id('campo-outro').hidden = !outro;
  id('erro-item').hidden = true;
  if (outro) id('outro').focus();
});

function ajustarQuantidade(delta) {
  const atual = Number(id('qtd').value) || 1;
  id('qtd').value = Math.min(999, Math.max(1, Math.round(atual) + delta));
  id('qtd').dispatchEvent(new Event('input', { bubbles: true }));
}
id('menos').addEventListener('click', () => ajustarQuantidade(-1));
id('mais').addEventListener('click', () => ajustarQuantidade(1));
id('qtd').addEventListener('focus', () => id('qtd').select());
id('qtd').addEventListener('input', () => {
  id('menos').disabled = (Number(id('qtd').value) || 1) <= 1;
});

id('email').addEventListener('blur', () => {
  const campo = id('email');
  if (campo.value.trim() && !campo.validity.valid) {
    mostrarErro('erro-email', 'Confira o e-mail: parece faltar algo (ex.: nome@email.com).', 'email');
  }
});

for (const campo of document.querySelectorAll('input')) {
  campo.addEventListener('input', () => {
    if (campo.getAttribute('aria-invalid') !== 'true') return;
    campo.removeAttribute('aria-invalid');
    const erro = document.getElementById(`erro-${campo.id === 'qtd' ? 'qtd' : campo.id}`);
    if (erro) erro.hidden = true;
  });
}

id('nao-sou-eu').addEventListener('click', () => {
  guardar(CHAVE_DOADOR, null);
  estado.usuarioId = null;
  id('nome').value = '';
  id('email').value = '';
  id('lembrado').hidden = true;
  id('nome').focus();
});

id('doar-de-novo').addEventListener('click', () => {
  id('sucesso').hidden = true;
  id('cartao').hidden = false;
  for (const radio of document.querySelectorAll('input[name="item"]')) radio.checked = false;
  id('outro').value = '';
  id('campo-outro').hidden = true;
  id('qtd').value = '1';
  id('menos').disabled = true;
  estado.item = '';
  irParaPasso(1);
  atualizarImpacto();
});

id('compartilhar').addEventListener('click', async () => {
  const dados = {
    title: 'Doe alimentos',
    text: 'Acabei de registrar uma doação de alimentos. Venha doar também!',
    url: window.location.href.split('?')[0],
  };
  try {
    if (navigator.share) await navigator.share(dados);
    else {
      await navigator.clipboard.writeText(dados.url);
      id('anuncio').textContent = 'Link copiado.';
      id('compartilhar').textContent = 'Link copiado';
    }
  } catch (erro) { /* cancelado pela pessoa */ }
});

/* ---------------- início ---------------- */

(function iniciar() {
  const hoje = new Date();
  const maximo = new Date();
  maximo.setDate(maximo.getDate() + 30);
  id('data').value = iso(hoje);
  id('data').max = iso(maximo);
  id('menos').disabled = true;

  const salvo = doadorLembrado();
  if (salvo) {
    id('nome').value = salvo.nome || '';
    id('email').value = salvo.email || '';
    id('lembrado-texto').textContent = `Continuando como ${primeiroNome(salvo.nome) || 'você'}.`;
    id('lembrado').hidden = false;
  }

  if (FORCAR_DEMO) ativarDemo();
  irParaPasso(1, { foco: false });
  carregarItens(); // também acorda a API gratuita em segundo plano
  atualizarImpacto();
})();
