document.addEventListener('DOMContentLoaded', () => {
    const historial = document.querySelector('#historial-table');
    if (historial && typeof DataTable !== 'undefined') {
        new DataTable(historial, {
            pageLength: 10,
            lengthMenu: [10, 25, 50, 100],
            order: [[1, 'desc'], [0, 'desc']],
            language: {
                search: 'Buscar:', lengthMenu: 'Mostrar _MENU_ ventas',
                info: 'Mostrando _START_ a _END_ de _TOTAL_ ventas',
                infoEmpty: 'No hay ventas', infoFiltered: '(de _MAX_ ventas en total)',
                emptyTable: 'Aún no hay ventas registradas.',
                zeroRecords: 'No se encontraron ventas con esa búsqueda.',
                paginate: { first: 'Primera', last: 'Última', next: 'Siguiente', previous: 'Anterior' }
            }
        });
    }
    const usuarios = document.querySelector('#usuarios-table');
    if (usuarios && typeof DataTable !== 'undefined') {
        new DataTable(usuarios, {
            columnDefs: [{ targets: -1, orderable: false, searchable: false }],
            pageLength: 10,
            lengthMenu: [10, 25, 50, 100],
            order: [[0, 'asc']],
            language: {
                search: 'Buscar:', lengthMenu: 'Mostrar _MENU_ usuarios',
                info: 'Mostrando _START_ a _END_ de _TOTAL_ usuarios',
                infoEmpty: 'No hay usuarios', infoFiltered: '(de _MAX_ usuarios en total)',
                emptyTable: 'Aún no hay usuarios. Usa Nuevo Usuario para comenzar.',
                zeroRecords: 'No se encontraron usuarios con esa búsqueda.',
                paginate: { first: 'Primera', last: 'Última', next: 'Siguiente', previous: 'Anterior' }
            }
        });
    }
    const table = document.querySelector('#productos-table');
    if (!table || typeof DataTable === 'undefined') return;
    new DataTable(table, {
        pageLength: 10,
        lengthMenu: [10, 25, 50, 100],
        order: [[0, 'desc']],
        columnDefs: [{ targets: -1, orderable: false, searchable: false }],
        language: {
            search: 'Buscar:', lengthMenu: 'Mostrar _MENU_ productos',
            info: 'Mostrando _START_ a _END_ de _TOTAL_ productos',
            infoEmpty: 'No hay productos', infoFiltered: '(de _MAX_ productos en total)',
            emptyTable: 'Aún no hay productos. Usa Nuevo producto para comenzar.',
            zeroRecords: 'No se encontraron productos con esa búsqueda.',
            paginate: { first: 'Primera', last: 'Última', next: 'Siguiente', previous: 'Anterior' }
        }
    });
});
