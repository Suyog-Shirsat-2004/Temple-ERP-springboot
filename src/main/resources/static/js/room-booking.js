(function () {
  'use strict';
  function byId(id) { return document.getElementById(id); }
  function inr(n) { n = isFinite(n) ? n : 0; return '₹' + n.toLocaleString('en-IN', { maximumFractionDigits: 2 }); }
  function nights() {
    var a = byId('check_in'), b = byId('check_out');
    if (!a || !b || !a.value || !b.value) return 1;
    var d1 = new Date(a.value), d2 = new Date(b.value);
    var diff = Math.round((d2 - d1) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
  }
  var selectedCard = null;
  function updateCalc() {
    var price = selectedCard ? (parseFloat(selectedCard.getAttribute('data-price') || '0') || 0) : 0;
    var n = nights();
    var cPrice = byId('cPrice'), cNights = byId('cNights'), cTotal = byId('cTotal');
    if (cPrice) cPrice.textContent = inr(price);
    if (cNights) cNights.textContent = String(n);
    if (cTotal) cTotal.textContent = inr(price * n);
  }
  function openDetails() {
    var panel = byId('step2Panel');
    if (!panel) return;
    panel.classList.add('show');
    var s2b = byId('step2Badge'); if (s2b) s2b.classList.add('active');
    try { panel.scrollIntoView({ behavior: 'smooth', block: 'start' }); } catch (e) { panel.scrollIntoView(true); }
    var g = byId('guest_name'); if (g) setTimeout(function () { try { g.focus(); } catch (e) {} }, 300);
  }
  function selectRoom(card) {
    if (!card) return;
    var cards = document.querySelectorAll('.room');
    for (var i = 0; i < cards.length; i++) {
      cards[i].classList.remove('selected');
      var tg = cards[i].querySelector('.tag');
      if (tg) tg.innerHTML = '<i class="fas fa-hand-pointer"></i> Select';
    }
    card.classList.add('selected');
    selectedCard = card;
    var tag = card.querySelector('.tag');
    if (tag) tag.innerHTML = '<i class="fas fa-check"></i> Selected';
    var rid = byId('room_id'); if (rid) rid.value = card.getAttribute('data-id') || '';
    var lbl = byId('selectedLabel'); if (lbl) lbl.textContent = card.getAttribute('data-label') || card.getAttribute('data-name') || 'Selected room';
    var cap = parseInt(card.getAttribute('data-capacity') || '', 10);
    var g = byId('no_of_guests');
    if (g) { if (isFinite(cap) && cap > 0) g.max = String(cap); else g.removeAttribute('max'); }
    var hint = byId('capacityHint');
    if (hint) hint.textContent = (isFinite(cap) && cap > 0) ? ('Room capacity: ' + cap + ' person(s). Please enter guests within capacity.') : '';
    var err = byId('capacityError'); if (err) { err.style.display = 'none'; err.textContent = ''; }
    var next = byId('nextBtn'); if (next) { next.disabled = false; next.style.opacity = '1'; next.style.cursor = 'pointer'; }
    var s1 = byId('step1Badge'); if (s1) { s1.classList.add('done'); s1.classList.remove('active'); }
    updateCalc();
    openDetails();
  }
  function backToRooms() {
    var panel = byId('step2Panel'); if (panel) panel.classList.remove('show');
    var s2b = byId('step2Badge'); if (s2b) s2b.classList.remove('active');
    var s1 = byId('step1Panel'); if (s1 && s1.scrollIntoView) { try { s1.scrollIntoView({ behavior: 'smooth', block: 'start' }); } catch (e) {} }
  }
  function toggleQr() {
    var pm = byId('payment_method');
    var v = pm ? (pm.value || '') : '';
    var show = v.indexOf('QR') > -1 || v.toUpperCase() === 'UPI';
    var el = byId('qrPayRoom'); if (el) el.style.display = show ? 'block' : 'none';
  }
  function validEmail(v) {
    if (!v) return false;
    return /^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$/.test(String(v).trim());
  }
  function showError(msg) {
    var err = byId('capacityError');
    if (err) { err.textContent = msg; err.style.display = 'block'; try { err.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch (e) {} }
  }
  function clearError() { var err = byId('capacityError'); if (err) { err.style.display = 'none'; err.textContent = ''; } }
  function checkCapacity() {
    var g = byId('no_of_guests');
    var guests = parseInt(g && g.value ? g.value : '1', 10);
    var cap = selectedCard ? parseInt(selectedCard.getAttribute('data-capacity') || '', 10) : NaN;
    if (isFinite(cap) && cap > 0 && guests > cap) { showError('Room capacity is ' + cap + ' person(s). Please enter guests within the room capacity.'); return false; }
    clearError(); return true;
  }
  function init() {
    window.__roomBookingReady = true;
    window.selectRoom = selectRoom;
    window.goToDetails = openDetails;
    window.backToRooms = backToRooms;
    window.toggleQr = toggleQr;
    window.updateCalc = updateCalc;

    document.addEventListener('click', function (ev) {
      var t = ev.target; if (!t || !t.closest) return;
      var card = t.closest('.room');
      if (card) { ev.preventDefault && ev.preventDefault(); selectRoom(card); }
    }, false);

    var grid = document.querySelector('.rooms-grid');
    if (grid) {
      grid.addEventListener('keydown', function (ev) {
        if (ev.key === 'Enter' || ev.key === ' ') {
          var card = ev.target && ev.target.closest ? ev.target.closest('.room') : null;
          if (card) { ev.preventDefault(); selectRoom(card); }
        }
      });
    }

    ['check_in', 'check_out', 'no_of_guests'].forEach(function (id) {
      var e = byId(id);
      if (e) { e.addEventListener('change', function () { updateCalc(); checkCapacity(); }); e.addEventListener('input', function () { updateCalc(); checkCapacity(); }); }
    });
    var phoneEl = byId('phone');
    if (phoneEl) { phoneEl.addEventListener('input', function () { var d = (this.value || '').replace(/\D/g, '').slice(0, 10); if (this.value !== d) this.value = d; }); }
    var bookingForm = byId('bookingForm');
    if (bookingForm) {
      bookingForm.addEventListener('submit', function (ev) {
        clearError();
        if (!selectedCard || !byId('room_id') || !byId('room_id').value) { ev.preventDefault(); showError('Please select a room first.'); return; }
        if (!checkCapacity()) { ev.preventDefault(); return; }
        var phone = (byId('phone') || {}).value || '';
        phone = String(phone).replace(/\D/g, '');
        if (!/^[0-9]{10}$/.test(phone)) { ev.preventDefault(); showError('Phone number must be exactly 10 digits (numbers only).'); var pe = byId('phone'); if (pe) pe.focus(); return; }
        var email = ((byId('email') || {}).value || '').trim();
        if (!validEmail(email)) { ev.preventDefault(); showError('Please enter a valid email address.'); var ee = byId('email'); if (ee) ee.focus(); return; }
      });
    }
    try {
      var params = new URLSearchParams(window.location.search);
      var rid = params.get('room_id');
      if (rid) { var el = document.querySelector('.room[data-id="' + rid + '"]'); if (el) selectRoom(el); }
    } catch (e) {}
    toggleQr();
    updateCalc();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
