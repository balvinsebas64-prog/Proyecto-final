document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('#cobrar-form');
    form?.addEventListener('submit', () => {
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = 'Procesando…';
    });

    const confirmation = document.querySelector('#venta-exitosa-aviso');
    if (!confirmation || !window.Swal) return;
    Swal.fire({
        icon: 'success',
        titleText: '¡Venta registrada!',
        text: confirmation.querySelector('span').textContent,
        confirmButtonText: 'Siguiente venta',
        confirmButtonColor: '#198754',
        animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        didOpen: () => confirmation.classList.add('d-none')
    }).then(() => {
        window.location.replace(confirmation.dataset.nextUrl);
    });
});
