(function () {
  'use strict';
  function byId(id) { return document.getElementById(id); }
  function inr(n) { n = isFinite(n) ? n : 0; return '\u20B9' + n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function esc(s){ return String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
  var cart = {}; // key: name|price -> {name, price, qty}
  function cartKey(name, price){ return String(name||'') + '|' + String(price==null?'0':price); }
  function cartItems(){ var arr=[]; for (var k in cart) { if (cart.hasOwnProperty(k)) arr.push(cart[k]); } return arr; }
  function cartTotal(){ var t=0; cartItems().forEach(function(it){ t += (parseFloat(it.price)||0) * (parseInt(it.qty,10)||0); }); return Math.round(t*100)/100; }
  function updateCalc() {
    var items = cartItems();
    var total = cartTotal();
    var cPrice = byId('cPrice'), cQty = byId('cQty'), cTotal = byId('cTotal'), qrAmount = byId('qrAmount');
    var totalQty = 0; items.forEach(function(it){ totalQty += (parseInt(it.qty,10)||0); });
    if (cPrice) cPrice.textContent = items.length === 1 ? inr(parseFloat(items[0].price)||0) : (items.length > 1 ? 'Multiple' : inr(0));
    if (cQty) cQty.textContent = String(totalQty || 0);
    if (cTotal) cTotal.textContent = inr(total);
    if (qrAmount) qrAmount.textContent = inr(total);
    var selectedLabel = byId('selectedLabel');
    if (selectedLabel) selectedLabel.textContent = items.length ? (items.length + ' item' + (items.length>1?'s':'') + ' selected') : '—';
    var itemDisplay = byId('item_display'), priceDisplay = byId('price_display');
    if (itemDisplay) itemDisplay.value = items.map(function(it){ return it.name + ' x' + it.qty; }).join(', ');
    if (priceDisplay) priceDisplay.value = inr(total);
    var next = byId('nextBtn');
    if (next) { var ok = items.length > 0; next.disabled = !ok; next.style.opacity = ok ? '1' : '.55'; next.style.cursor = ok ? 'pointer' : 'not-allowed'; }
    renderCart();
    syncFormInputs();
  }
  function renderCart(){
    var wrap = byId('cartWrap'); if(!wrap) return;
    var items = cartItems();
    if(!items.length){ wrap.innerHTML = '<div style="color:#4f6b63;font-size:13px">No items added yet. Click menu items to add them to this order.</div>'; return; }
    var html = '<div style="border:1px solid #cfe3d6;border-radius:12px;overflow:hidden;background:#fff"><table style="width:100%;border-collapse:collapse;font-size:14px"><thead><tr style="background:#ecfdf5;color:#166534"><th style="text-align:left;padding:8px 10px">Item</th><th style="text-align:right;padding:8px 10px">Price</th><th style="text-align:center;padding:8px 10px">Qty</th><th style="text-align:right;padding:8px 10px">Line Total</th><th style="padding:8px 10px"></th></tr></thead><tbody>';
    items.forEach(function(it){
      var k = cartKey(it.name, it.price);
      var line = (parseFloat(it.price)||0) * (parseInt(it.qty,10)||0);
      html += '<tr>'
        + '<td style="padding:8px 10px;border-top:1px solid #e6f3ec;font-weight:600">'+esc(it.name)+'</td>'
        + '<td style="padding:8px 10px;border-top:1px solid #e6f3ec;text-align:right">'+inr(parseFloat(it.price)||0)+'</td>'
        + '<td style="padding:8px 10px;border-top:1px solid #e6f3ec;text-align:center">'
        +   '<button type="button" data-k="'+esc(k)+'" data-act="dec" style="border:1px solid #cbd5e1;background:#fff;border-radius:6px;padding:2px 8px;cursor:pointer">-</button>'
        +   ' <strong style="display:inline-block;min-width:22px">'+(parseInt(it.qty,10)||0)+'</strong> '
        +   '<button type="button" data-k="'+esc(k)+'" data-act="inc" style="border:1px solid #cbd5e1;background:#fff;border-radius:6px;padding:2px 8px;cursor:pointer">+</button>'
        + '</td>'
        + '<td style="padding:8px 10px;border-top:1px solid #e6f3ec;text-align:right;font-weight:700;color:#166534">'+inr(line)+'</td>'
        + '<td style="padding:8px 10px;border-top:1px solid #e6f3ec;text-align:center"><button type="button" data-k="'+esc(k)+'" data-act="rm" style="border:1px solid #fecaca;background:#fff5f5;color:#991b1b;border-radius:6px;padding:4px 8px;cursor:pointer">Remove</button></td>'
        + '</tr>';
    });
    html += '</tbody></table></div>';
    wrap.innerHTML = html;
  }
  function syncFormInputs(){
    var container = byId('cartInputs'); if(!container) return;
    container.innerHTML = '';
    cartItems().forEach(function(it){
      [['item_name', it.name], ['item_price', it.price], ['qty', it.qty]].forEach(function(pair){
        var inp = document.createElement('input');
        inp.type = 'hidden'; inp.name = pair[0]; inp.value = pair[1] == null ? '' : pair[1];
        container.appendChild(inp);
      });
    });
  }
  function addOrIncrement(card){
    if(!card) return;
    var name = card.getAttribute('data-name') || '';
    var price = parseFloat(card.getAttribute('data-price') || '0') || 0;
    if(!name) return;
    var k = cartKey(name, price);
    if(!cart[k]) cart[k] = { name: name, price: price, qty: 0 };
    cart[k].qty = (parseInt(cart[k].qty,10)||0) + 1;
    if(cart[k].qty > 50) cart[k].qty = 50;
    updateSelectionUI();
    updateCalc();
    goToPayment();
  }
  function updateSelectionUI(){
    var cards = document.querySelectorAll('.item');
    for (var i=0;i<cards.length;i++){
      var c = cards[i];
      var name = c.getAttribute('data-name')||'';
      var price = parseFloat(c.getAttribute('data-price')||'0')||0;
      var k = cartKey(name, price);
      var inCart = !!(cart[k] && cart[k].qty > 0);
      c.classList.toggle('selected', inCart);
      var tg = c.querySelector('.tag');
      if(tg) tg.innerHTML = inCart ? ('<i class="fas fa-check"></i> Added ('+cart[k].qty+')') : '<i class="fas fa-hand-pointer"></i> Add';
    }
    var s1 = byId('step1Badge');
    if(s1){ if(cartItems().length){ s1.classList.add('done'); s1.classList.remove('active'); } else { s1.classList.remove('done'); s1.classList.add('active'); } }
  }
  function renderPayment() {
    var paySel = byId('payment_method'), payBadge = byId('payBadge'), payTitle = byId('payTitle'), payDesc = byId('payDesc'), payStage = byId('payStage'), qrWrap = byId('qrWrap');
    var method = paySel ? paySel.value : 'UPI QR';
    var isCash = /cash/i.test(method);
    if (payBadge) payBadge.innerHTML = isCash ? '<i class="fas fa-coins"></i> Cash' : '<i class="fas fa-qrcode"></i> UPI / QR';
    if (payTitle) payTitle.textContent = isCash ? 'Cash Payment' : 'Digital Payment';
    if (payDesc) payDesc.textContent = isCash ? 'Pay the total amount at the counter/cash on delivery flow as applicable, then place your order.' : 'Scan the QR, verify the amount, complete payment, then place your order.';
    if (qrWrap) qrWrap.style.display = isCash ? 'none' : 'block';
    if (!payStage) return;
    if (isCash) { payStage.innerHTML = '<div class="coin" style="left:24%">\u20B9</div><div class="coin b" style="left:47%">\u20B9</div><div class="coin c" style="left:69%">\u20B9</div><div class="bar"></div>'; }
    else { payStage.innerHTML = '<div class="scan"><i class="fas fa-qrcode"></i><div class="beam"></div></div><div class="phone"><i class="fas fa-mobile-alt"></i></div>'; }
  }
  function goToPayment() {
    if (!cartItems().length) return;
    var panel = byId('step2Panel'); if (!panel) return;
    panel.classList.add('show');
    var s2 = byId('step2Badge'); if (s2) s2.classList.add('active');
    try { panel.scrollIntoView({ behavior: 'smooth', block: 'start' }); } catch (e) { panel.scrollIntoView(true); }
  }
  function backToMenu() {
    var panel = byId('step2Panel'); if (panel) panel.classList.remove('show');
    var s2 = byId('step2Badge'); if (s2) s2.classList.remove('active');
    var s1 = byId('step1Panel'); if (s1 && s1.scrollIntoView) try { s1.scrollIntoView({ behavior: 'smooth', block: 'start' }); } catch (e) {}
  }
  function selectItem(card){ addOrIncrement(card); } // compatibility: clicking adds item
  function init() {
    window.__restaurantReady = true;
    window.selectItem = selectItem;
    window.goToPayment = goToPayment;
    window.backToMenu = backToMenu;
    window.renderPayment = renderPayment;
    window.updateCalc = updateCalc;
    document.addEventListener('click', function (ev) {
      var t = ev.target; if (!t) return;
      var cartBtn = t.closest ? t.closest('#cartWrap [data-act]') : null;
      if (cartBtn) {
        var k = cartBtn.getAttribute('data-k'); var act = cartBtn.getAttribute('data-act');
        if (k && cart[k]) {
          if (act === 'inc') cart[k].qty = Math.min(50, (parseInt(cart[k].qty,10)||0) + 1);
          else if (act === 'dec') cart[k].qty = (parseInt(cart[k].qty,10)||0) - 1;
          else if (act === 'rm') cart[k].qty = 0;
          if ((parseInt(cart[k].qty,10)||0) <= 0) delete cart[k];
          updateSelectionUI(); updateCalc();
        }
        ev.preventDefault(); return;
      }
      var card = t.closest ? t.closest('.item') : null;
      if (card) addOrIncrement(card);
    }, false);
    var pm = byId('payment_method'); if (pm) pm.addEventListener('change', renderPayment);
    var form = byId('orderForm');
    if (form) form.addEventListener('submit', function(e){
      syncFormInputs();
      if(!cartItems().length){ e.preventDefault(); alert('Please add at least one item to the order.'); }
    });
    renderPayment();
    updateSelectionUI();
    updateCalc();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
