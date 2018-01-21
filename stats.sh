#!/usr/bin/env bash
echo level 0: `grep level\":0 db/clients/[0-9]*.db | wc -l`
echo level 1: `grep level\":1 db/clients/[0-9]*.db | wc -l`
echo level 2: `grep level\":2 db/clients/[0-9]*.db | wc -l`
echo level 3: `grep level\":3 db/clients/[0-9]*.db | wc -l`
echo level 4: `grep level\":4 db/clients/[0-9]*.db | wc -l`
echo level 5: `grep level\":5 db/clients/[0-9]*.db | wc -l`
echo level 6: `grep level\":6 db/clients/[0-9]*.db | wc -l`
echo level 7: `grep level\":7 db/clients/[0-9]*.db | wc -l`
echo level 8: `grep level\":8 db/clients/[0-9]*.db | wc -l`
