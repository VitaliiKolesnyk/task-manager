import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Task } from '../task.model';
import { TaskService } from '../task.service';

interface EditState {
  id: number;
  title: string;
  description: string;
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tasks.component.html',
  styleUrls: ['./tasks.component.css'],
})
export class TasksComponent implements OnInit {
  private readonly service = inject(TaskService);

  readonly tasks = signal<Task[]>([]);
  readonly showCompleted = signal(false);
  readonly newTitle = signal('');
  readonly newDescription = signal('');
  readonly editing = signal<EditState | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.service.list(this.showCompleted()).subscribe((data) => this.tasks.set(data));
  }

  toggleShowCompleted(): void {
    this.showCompleted.set(!this.showCompleted());
    this.refresh();
  }

  add(): void {
    const title = this.newTitle().trim();
    if (!title) return;
    this.service.create({ title, description: this.newDescription().trim() || null }).subscribe(() => {
      this.newTitle.set('');
      this.newDescription.set('');
      this.refresh();
    });
  }

  markDone(task: Task, done: boolean): void {
    this.service.update(task.id, { done }).subscribe(() => this.refresh());
  }

  startEdit(task: Task): void {
    this.editing.set({ id: task.id, title: task.title, description: task.description ?? '' });
  }

  cancelEdit(): void {
    this.editing.set(null);
  }

  saveEdit(): void {
    const state = this.editing();
    if (!state) return;
    this.service
      .update(state.id, { title: state.title.trim(), description: state.description.trim() || null })
      .subscribe(() => {
        this.editing.set(null);
        this.refresh();
      });
  }

  remove(task: Task): void {
    this.service.remove(task.id).subscribe(() => this.refresh());
  }

  updateEditTitle(value: string): void {
    const state = this.editing();
    if (state) this.editing.set({ ...state, title: value });
  }

  updateEditDescription(value: string): void {
    const state = this.editing();
    if (state) this.editing.set({ ...state, description: value });
  }
}
